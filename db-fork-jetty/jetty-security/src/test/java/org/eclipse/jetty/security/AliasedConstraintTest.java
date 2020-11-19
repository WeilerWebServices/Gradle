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

package org.eclipse.jetty.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Some requests for static data that is served by ResourceHandler, but some is secured.
 * <p>
 * This is mainly here to test security bypass techniques using aliased names that should be caught.
 */
public class AliasedConstraintTest
{
    private static final String TEST_REALM = "TestRealm";
    private static Server server;
    private static LocalConnector connector;
    private static ConstraintSecurityHandler security;

    @BeforeAll
    public static void startServer() throws Exception
    {
        server = new Server();
        connector = new LocalConnector(server);
        server.setConnectors(new Connector[] { connector });

        ContextHandler context = new ContextHandler();
        SessionHandler session = new SessionHandler();

        TestLoginService loginService = new TestLoginService(TEST_REALM);

        loginService.putUser("user0",new Password("password"),new String[] {});
        loginService.putUser("user",new Password("password"),new String[] { "user" });
        loginService.putUser("user2",new Password("password"),new String[] { "user" });
        loginService.putUser("admin",new Password("password"),new String[] { "user", "administrator" });
        loginService.putUser("user3",new Password("password"),new String[] { "foo" });

        context.setContextPath("/ctx");
        context.setResourceBase(MavenTestingUtils.getTestResourceDir("docroot").getAbsolutePath());
        
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{context,new DefaultHandler()});
        server.setHandler(handlers);
        context.setHandler(session);
        // context.addAliasCheck(new AllowSymLinkAliasChecker());

        server.addBean(loginService);

        security = new ConstraintSecurityHandler();
        session.setHandler(security);
        ResourceHandler handler = new ResourceHandler();
        security.setHandler(handler);

        List<ConstraintMapping> constraints = new ArrayList<>();

        Constraint constraint0 = new Constraint();
        constraint0.setAuthenticate(true);
        constraint0.setName("forbid");
        ConstraintMapping mapping0 = new ConstraintMapping();
        mapping0.setPathSpec("/forbid/*");
        mapping0.setConstraint(constraint0);
        constraints.add(mapping0);

        Set<String> knownRoles = new HashSet<>();
        knownRoles.add("user");
        knownRoles.add("administrator");

        security.setConstraintMappings(constraints,knownRoles);
        server.start();
    }

    @AfterAll
    public static void stopServer() throws Exception
    {
        server.stop();
    }

    public static Stream<Arguments> data()
    {
        List<Object[]> data = new ArrayList<>();

        final String OPENCONTENT = "this is open content";

        data.add(new Object[] { "/ctx/all/index.txt", HttpStatus.OK_200, OPENCONTENT });
        data.add(new Object[] { "/ctx/ALL/index.txt", HttpStatus.NOT_FOUND_404, null });
        data.add(new Object[] { "/ctx/ALL/Fred/../index.txt", HttpStatus.NOT_FOUND_404, null });
        data.add(new Object[] { "/ctx/../bar/../ctx/all/index.txt", HttpStatus.OK_200, OPENCONTENT });
        data.add(new Object[] { "/ctx/forbid/index.txt", HttpStatus.FORBIDDEN_403, null });
        data.add(new Object[] { "/ctx/all/../forbid/index.txt", HttpStatus.FORBIDDEN_403, null });
        data.add(new Object[] { "/ctx/FoRbId/index.txt", HttpStatus.NOT_FOUND_404, null });

        return data.stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testAccess(String uri, int expectedStatusCode, String expectedContent) throws Exception
    {
        StringBuilder request = new StringBuilder();
        request.append("GET ").append(uri).append(" HTTP/1.1\r\n");
        request.append("Host: localhost\r\n");
        request.append("Connection: close\r\n");
        request.append("\r\n");

        String response = connector.getResponse(request.toString());

        switch (expectedStatusCode)
        {
            case 200:
                assertThat(response,startsWith("HTTP/1.1 200 OK"));
                break;
            case 403:
                assertThat(response,startsWith("HTTP/1.1 403 Forbidden"));
                break;
            case 404:
                assertThat(response,startsWith("HTTP/1.1 404 Not Found"));
                break;
            default:
                fail("Write a handler for response status code: " + expectedStatusCode);
                break;
        }

        if (expectedContent != null)
        {
            assertThat(response,containsString("this is open content"));
        }
    }
}
