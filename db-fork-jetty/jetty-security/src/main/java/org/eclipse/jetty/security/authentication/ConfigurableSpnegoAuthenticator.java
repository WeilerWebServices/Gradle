//
//  ========================================================================
//  Copyright (c) 1995-2019 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.security.authentication;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.SpnegoUserIdentity;
import org.eclipse.jetty.security.SpnegoUserPrincipal;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Authentication.User;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.security.Constraint;

/**
 * <p>A LoginAuthenticator that uses SPNEGO and the GSS API to authenticate requests.</p>
 * <p>A successful authentication from a client is cached for a configurable
 * {@link #getAuthenticationDuration() duration} using the HTTP session; this avoids
 * that the client is asked to authenticate for every request.</p>
 *
 * @see org.eclipse.jetty.security.ConfigurableSpnegoLoginService
 */
public class ConfigurableSpnegoAuthenticator extends LoginAuthenticator
{
    private static final Logger LOG = Log.getLogger(ConfigurableSpnegoAuthenticator.class);

    private final String _authMethod;
    private Duration _authenticationDuration = Duration.ofNanos(-1);

    public ConfigurableSpnegoAuthenticator()
    {
        this(Constraint.__SPNEGO_AUTH);
    }

    /**
     * Allow for a custom authMethod value to be set for instances where SPNEGO may not be appropriate
     *
     * @param authMethod the auth method
     */
    public ConfigurableSpnegoAuthenticator(String authMethod)
    {
        _authMethod = authMethod;
    }

    @Override
    public String getAuthMethod()
    {
        return _authMethod;
    }

    /**
     * @return the authentication duration
     */
    public Duration getAuthenticationDuration()
    {
        return _authenticationDuration;
    }

    /**
     * <p>Sets the duration of the authentication.</p>
     * <p>A negative duration means that the authentication is only valid for the current request.</p>
     * <p>A zero duration means that the authentication is valid forever.</p>
     * <p>A positive value means that the authentication is valid for the specified duration.</p>
     *
     * @param authenticationDuration the authentication duration
     */
    public void setAuthenticationDuration(Duration authenticationDuration)
    {
        _authenticationDuration = authenticationDuration;
    }

    @Override
    public Authentication validateRequest(ServletRequest req, ServletResponse res, boolean mandatory) throws ServerAuthException
    {
        if (!mandatory)
            return new DeferredAuthentication(this);

        HttpServletRequest request = (HttpServletRequest)req;
        HttpServletResponse response = (HttpServletResponse)res;

        String header = request.getHeader(HttpHeader.AUTHORIZATION.asString());
        String spnegoToken = getSpnegoToken(header);
        HttpSession httpSession = request.getSession(false);

        // We have a token from the client, so run the login.
        if (header != null && spnegoToken != null)
        {
            SpnegoUserIdentity identity = (SpnegoUserIdentity)login(null, spnegoToken, request);
            if (identity.isEstablished())
            {
                if (!DeferredAuthentication.isDeferred(response))
                {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Sending final token");
                    // Send to the client the final token so that the
                    // client can establish the GSS context with the server.
                    SpnegoUserPrincipal principal = (SpnegoUserPrincipal)identity.getUserPrincipal();
                    setSpnegoToken(response, principal.getEncodedToken());
                }

                Duration authnDuration = getAuthenticationDuration();
                if (!authnDuration.isNegative())
                {
                    if (httpSession == null)
                        httpSession = request.getSession(true);
                    httpSession.setAttribute(UserIdentityHolder.ATTRIBUTE, new UserIdentityHolder(identity));
                }
                return new UserAuthentication(getAuthMethod(), identity);
            }
            else
            {
                if (DeferredAuthentication.isDeferred(response))
                    return Authentication.UNAUTHENTICATED;
                if (LOG.isDebugEnabled())
                    LOG.debug("Sending intermediate challenge");
                SpnegoUserPrincipal principal = (SpnegoUserPrincipal)identity.getUserPrincipal();
                sendChallenge(response, principal.getEncodedToken());
                return Authentication.SEND_CONTINUE;
            }
        }
        // No token from the client; check if the client has logged in
        // successfully before and the authentication has not expired.
        else if (httpSession != null)
        {
            UserIdentityHolder holder = (UserIdentityHolder)httpSession.getAttribute(UserIdentityHolder.ATTRIBUTE);
            if (holder != null)
            {
                UserIdentity identity = holder._userIdentity;
                if (identity != null)
                {
                    Duration authnDuration = getAuthenticationDuration();
                    if (!authnDuration.isNegative())
                    {
                        boolean expired = !authnDuration.isZero() && Instant.now().isAfter(holder._validFrom.plus(authnDuration));
                        // Allow non-GET requests even if they're expired, so that
                        // the client does not need to send the request content again.
                        if (!expired || !HttpMethod.GET.is(request.getMethod()))
                            return new UserAuthentication(getAuthMethod(), identity);
                    }
                }
            }
        }

        if (DeferredAuthentication.isDeferred(response))
            return Authentication.UNAUTHENTICATED;

        if (LOG.isDebugEnabled())
            LOG.debug("Sending initial challenge");
        sendChallenge(response, null);
        return Authentication.SEND_CONTINUE;
    }

    private void sendChallenge(HttpServletResponse response, String token) throws ServerAuthException
    {
        try
        {
            setSpnegoToken(response, token);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
        catch (IOException x)
        {
            throw new ServerAuthException(x);
        }
    }

    private void setSpnegoToken(HttpServletResponse response, String token)
    {
        String value = HttpHeader.NEGOTIATE.asString();
        if (token != null)
            value += " " + token;
        response.setHeader(HttpHeader.WWW_AUTHENTICATE.asString(), value);
    }

    private String getSpnegoToken(String header)
    {
        if (header == null)
            return null;
        String scheme = HttpHeader.NEGOTIATE.asString() + " ";
        if (header.regionMatches(true, 0, scheme, 0, scheme.length()))
            return header.substring(scheme.length()).trim();
        return null;
    }

    @Override
    public boolean secureResponse(ServletRequest request, ServletResponse response, boolean mandatory, User validatedUser)
    {
        return true;
    }

    private static class UserIdentityHolder implements Serializable
    {
        private static final String ATTRIBUTE = UserIdentityHolder.class.getName();

        private transient final Instant _validFrom = Instant.now();
        private transient final UserIdentity _userIdentity;

        private UserIdentityHolder(UserIdentity userIdentity)
        {
            _userIdentity = userIdentity;
        }
    }
}
