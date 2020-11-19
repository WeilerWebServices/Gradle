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

package org.eclipse.jetty.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

public class HttpCookieTest extends AbstractHttpClientServerTest
{
    private static final Cookie[] EMPTY_COOKIES = new Cookie[0];

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_CookieIsStored(Scenario scenario) throws Exception
    {
        final String name = "foo";
        final String value = "bar";
        start(scenario, new EmptyServerHandler()
        {
            @Override
            protected void service(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                response.addCookie(new Cookie(name, value));
            }
        });

        String host = "localhost";
        int port = connector.getLocalPort();
        String path = "/path";
        String uri = scenario.getScheme() + "://" + host + ":" + port + path;
        Response response = client.GET(uri);
        assertEquals(200, response.getStatus());

        List<HttpCookie> cookies = client.getCookieStore().get(URI.create(uri));
        assertNotNull(cookies);
        assertEquals(1, cookies.size());
        HttpCookie cookie = cookies.get(0);
        assertEquals(name, cookie.getName());
        assertEquals(value, cookie.getValue());
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_CookieIsSent(Scenario scenario) throws Exception
    {
        final String name = "foo";
        final String value = "bar";
        start(scenario, new EmptyServerHandler()
        {
            @Override
            protected void service(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                Cookie[] cookies = request.getCookies();
                assertNotNull(cookies);
                assertEquals(1, cookies.length);
                Cookie cookie = cookies[0];
                assertEquals(name, cookie.getName());
                assertEquals(value, cookie.getValue());
            }
        });

        String host = "localhost";
        int port = connector.getLocalPort();
        String path = "/path";
        String uri = scenario.getScheme() + "://" + host + ":" + port;
        HttpCookie cookie = new HttpCookie(name, value);
        client.getCookieStore().add(URI.create(uri), cookie);

        Response response = client.GET(scenario.getScheme() + "://" + host + ":" + port + path);
        assertEquals(200, response.getStatus());
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_CookieWithoutValue(Scenario scenario) throws Exception
    {
        start(scenario, new EmptyServerHandler()
        {
            @Override
            protected void service(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                response.addHeader("Set-Cookie", "");
            }
        });

        ContentResponse response = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .send();
        assertEquals(200, response.getStatus());
        assertTrue(client.getCookieStore().getCookies().isEmpty());
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_PerRequestCookieIsSent(Scenario scenario) throws Exception
    {
        final String name = "foo";
        final String value = "bar";
        start(scenario, new EmptyServerHandler()
        {
            @Override
            protected void service(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                Cookie[] cookies = request.getCookies();
                assertNotNull(cookies);
                assertEquals(1, cookies.length);
                Cookie cookie = cookies[0];
                assertEquals(name, cookie.getName());
                assertEquals(value, cookie.getValue());
            }
        });

        ContentResponse response = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .cookie(new HttpCookie(name, value))
                .timeout(5, TimeUnit.SECONDS)
                .send();
        assertEquals(200, response.getStatus());
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_SetCookieWithoutPath_RequestURIWithOneSegment(Scenario scenario) throws Exception
    {
        String headerName = "X-Request";
        String cookieName = "a";
        String cookieValue = "1";
        start(scenario, new EmptyServerHandler()
        {
            @Override
            protected void service(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                int r = request.getIntHeader(headerName);
                if ("/foo".equals(target) && r == 0)
                {
                    Cookie cookie = new Cookie(cookieName, cookieValue);
                    response.addCookie(cookie);
                }
                else
                {
                    Cookie[] cookies = Optional.ofNullable(request.getCookies()).orElse(EMPTY_COOKIES);
                    switch (target)
                    {
                        case "/":
                        case "/foo":
                        case "/foo/bar":
                            assertEquals(1, cookies.length, target);
                            Cookie cookie = cookies[0];
                            assertEquals(cookieName, cookie.getName(), target);
                            assertEquals(cookieValue, cookie.getValue(), target);
                            break;
                        default:
                            fail("Unrecognized target: " + target);
                    }
                }
            }
        });

        ContentResponse response = send(client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .path("/foo")
                .header(headerName, "0")
                .timeout(5, TimeUnit.SECONDS));
        assertEquals(HttpStatus.OK_200, response.getStatus());

        Arrays.asList("/", "/foo", "/foo/bar").forEach(path ->
        {
            ContentResponse r = send(client.newRequest("localhost", connector.getLocalPort())
                    .scheme(scenario.getScheme())
                    .path(path)
                    .header(headerName, "1")
                    .timeout(5, TimeUnit.SECONDS));
            assertEquals(HttpStatus.OK_200, r.getStatus());
        });
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_SetCookieWithoutPath_RequestURIWithTwoSegments(Scenario scenario) throws Exception
    {
        String headerName = "X-Request";
        String cookieName = "a";
        String cookieValue = "1";
        start(scenario, new EmptyServerHandler()
        {
            @Override
            protected void service(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                int r = request.getIntHeader(headerName);
                if ("/foo/bar".equals(target) && r == 0)
                {
                    Cookie cookie = new Cookie(cookieName, cookieValue);
                    response.addCookie(cookie);
                }
                else
                {
                    Cookie[] cookies = Optional.ofNullable(request.getCookies()).orElse(EMPTY_COOKIES);
                    switch (target)
                    {
                        case "/":
                        case "/foo":
                        case "/foobar":
                            assertEquals(0, cookies.length, target);
                            break;
                        case "/foo/":
                        case "/foo/bar":
                        case "/foo/bar/baz":
                            assertEquals(1, cookies.length, target);
                            Cookie cookie = cookies[0];
                            assertEquals(cookieName, cookie.getName(), target);
                            assertEquals(cookieValue, cookie.getValue(), target);
                            break;
                        default:
                            fail("Unrecognized Target: " + target);
                    }
                }
            }
        });

        ContentResponse response = send(client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .path("/foo/bar")
                .header(headerName, "0")
                .timeout(5, TimeUnit.SECONDS));
        assertEquals(HttpStatus.OK_200, response.getStatus());

        Arrays.asList("/", "/foo", "/foo/", "/foobar", "/foo/bar", "/foo/bar/baz").forEach(path ->
        {
            ContentResponse r = send(client.newRequest("localhost", connector.getLocalPort())
                    .scheme(scenario.getScheme())
                    .path(path)
                    .header(headerName, "1")
                    .timeout(5, TimeUnit.SECONDS));
            assertEquals(HttpStatus.OK_200, r.getStatus());
        });
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_SetCookieWithLongerPath(Scenario scenario) throws Exception
    {
        String headerName = "X-Request";
        String cookieName = "a";
        String cookieValue = "1";
        start(scenario, new EmptyServerHandler()
        {
            @Override
            protected void service(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                int r = request.getIntHeader(headerName);
                if ("/foo".equals(target) && r == 0)
                {
                    Cookie cookie = new Cookie(cookieName, cookieValue);
                    cookie.setPath("/foo/bar");
                    response.addCookie(cookie);
                }
                else
                {
                    Cookie[] cookies = Optional.ofNullable(request.getCookies()).orElse(EMPTY_COOKIES);
                    switch (target)
                    {
                        case "/":
                        case "/foo":
                        case "/foo/barbaz":
                            assertEquals(0, cookies.length, target);
                            break;
                        case "/foo/bar":
                        case "/foo/bar/":
                            assertEquals(1, cookies.length, target);
                            Cookie cookie = cookies[0];
                            assertEquals(cookieName, cookie.getName(), target);
                            assertEquals(cookieValue, cookie.getValue(), target);
                            break;
                        default:
                            fail("Unrecognized Target: " + target);
                    }
                }
            }
        });

        ContentResponse response = send(client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .path("/foo")
                .header(headerName, "0")
                .timeout(5, TimeUnit.SECONDS));
        assertEquals(HttpStatus.OK_200, response.getStatus());

        Arrays.asList("/", "/foo", "/foo/bar", "/foo/bar/", "/foo/barbaz").forEach(path ->
        {
            ContentResponse r = send(client.newRequest("localhost", connector.getLocalPort())
                    .scheme(scenario.getScheme())
                    .path(path)
                    .header(headerName, "1")
                    .timeout(5, TimeUnit.SECONDS));
            assertEquals(HttpStatus.OK_200, r.getStatus());
        });
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_SetCookieWithShorterPath(Scenario scenario) throws Exception
    {
        String headerName = "X-Request";
        String cookieName = "a";
        String cookieValue = "1";
        start(scenario, new EmptyServerHandler()
        {
            @Override
            protected void service(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                int r = request.getIntHeader(headerName);
                if ("/foo/bar".equals(target) && r == 0)
                {
                    Cookie cookie = new Cookie(cookieName, cookieValue);
                    cookie.setPath("/foo");
                    response.addCookie(cookie);
                }
                else
                {
                    Cookie[] cookies = Optional.ofNullable(request.getCookies()).orElse(EMPTY_COOKIES);
                    switch (target)
                    {
                        case "/":
                        case "/foobar":
                            assertEquals(0, cookies.length, target);
                            break;
                        case "/foo":
                        case "/foo/":
                        case "/foo/bar":
                            assertEquals(1, cookies.length, target);
                            Cookie cookie = cookies[0];
                            assertEquals(cookieName, cookie.getName(), target);
                            assertEquals(cookieValue, cookie.getValue(), target);
                            break;
                        default:
                            fail("Unrecognized Target: " + target);
                    }
                }
            }
        });

        ContentResponse response = send(client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .path("/foo/bar")
                .header(headerName, "0")
                .timeout(5, TimeUnit.SECONDS));
        assertEquals(HttpStatus.OK_200, response.getStatus());

        Arrays.asList("/", "/foo", "/foo/", "/foobar", "/foo/bar").forEach(path ->
        {
            ContentResponse r = send(client.newRequest("localhost", connector.getLocalPort())
                    .scheme(scenario.getScheme())
                    .path(path)
                    .header(headerName, "1")
                    .timeout(5, TimeUnit.SECONDS));
            assertEquals(HttpStatus.OK_200, r.getStatus());
        });
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_TwoSetCookieWithSameNameSamePath(Scenario scenario) throws Exception
    {
        String headerName = "X-Request";
        String cookieName = "a";
        String cookieValue1 = "1";
        String cookieValue2 = "2";
        start(scenario, new EmptyServerHandler()
        {
            @Override
            protected void service(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                int r = request.getIntHeader(headerName);
                if ("/foo".equals(target) && r == 0)
                {
                    Cookie cookie = new Cookie(cookieName, cookieValue1);
                    cookie.setPath("/foo");
                    response.addCookie(cookie);
                    cookie = new Cookie(cookieName, cookieValue2);
                    cookie.setPath("/foo");
                    response.addCookie(cookie);
                }
                else
                {
                    Cookie[] cookies = Optional.ofNullable(request.getCookies()).orElse(EMPTY_COOKIES);
                    switch (target)
                    {
                        case "/":
                            assertEquals(0, cookies.length, target);
                            break;
                        case "/foo":
                        case "/foo/bar":
                            assertEquals(1, cookies.length, target);
                            Cookie cookie = cookies[0];
                            assertEquals(cookieName, cookie.getName(), target);
                            assertEquals(cookieValue2, cookie.getValue(), target);
                            break;
                        default:
                            fail("Unrecognized Target: " + target);
                    }
                }
            }
        });

        ContentResponse response = send(client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .path("/foo")
                .header(headerName, "0")
                .timeout(5, TimeUnit.SECONDS));
        assertEquals(HttpStatus.OK_200, response.getStatus());

        Arrays.asList("/", "/foo", "/foo/bar").forEach(path ->
        {
            ContentResponse r = send(client.newRequest("localhost", connector.getLocalPort())
                    .scheme(scenario.getScheme())
                    .path(path)
                    .header(headerName, "1")
                    .timeout(5, TimeUnit.SECONDS));
            assertEquals(HttpStatus.OK_200, r.getStatus());
        });
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_TwoSetCookieWithSameNameDifferentPath(Scenario scenario) throws Exception
    {
        String headerName = "X-Request";
        String cookieName = "a";
        String cookieValue1 = "1";
        String cookieValue2 = "2";
        start(scenario, new EmptyServerHandler()
        {
            @Override
            protected void service(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                int r = request.getIntHeader(headerName);
                if ("/foo".equals(target) && r == 0)
                {
                    Cookie cookie = new Cookie(cookieName, cookieValue1);
                    cookie.setPath("/foo");
                    response.addCookie(cookie);
                    cookie = new Cookie(cookieName, cookieValue2);
                    cookie.setPath("/bar");
                    response.addCookie(cookie);
                }
                else
                {
                    Cookie[] cookies = Optional.ofNullable(request.getCookies()).orElse(EMPTY_COOKIES);
                    switch (target)
                    {
                        case "/":
                            assertEquals(0, cookies.length, target);
                            break;
                        case "/foo":
                        case "/foo/bar":
                            assertEquals(1, cookies.length, target);
                            Cookie cookie1 = cookies[0];
                            assertEquals(cookieName, cookie1.getName(), target);
                            assertEquals(cookieValue1, cookie1.getValue(), target);
                            break;
                        case "/bar":
                        case "/bar/foo":
                            assertEquals(1, cookies.length, target);
                            Cookie cookie2 = cookies[0];
                            assertEquals(cookieName, cookie2.getName(), target);
                            assertEquals(cookieValue2, cookie2.getValue(), target);
                            break;
                        default:
                            fail("Unrecognized Target: " + target);
                    }
                }
            }
        });

        ContentResponse response = send(client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .path("/foo")
                .header(headerName, "0")
                .timeout(5, TimeUnit.SECONDS));
        assertEquals(HttpStatus.OK_200, response.getStatus());

        Arrays.asList("/", "/foo", "/foo/bar", "/bar", "/bar/foo").forEach(path ->
        {
            ContentResponse r = send(client.newRequest("localhost", connector.getLocalPort())
                    .scheme(scenario.getScheme())
                    .path(path)
                    .header(headerName, "1")
                    .timeout(5, TimeUnit.SECONDS));
            assertEquals(HttpStatus.OK_200, r.getStatus());
        });
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_TwoSetCookieWithSameNamePath1PrefixOfPath2(Scenario scenario) throws Exception
    {
        String headerName = "X-Request";
        String cookieName = "a";
        String cookieValue1 = "1";
        String cookieValue2 = "2";
        start(scenario, new EmptyServerHandler()
        {
            @Override
            protected void service(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                int r = request.getIntHeader(headerName);
                if ("/foo".equals(target) && r == 0)
                {
                    Cookie cookie = new Cookie(cookieName, cookieValue1);
                    cookie.setPath("/foo");
                    response.addCookie(cookie);
                    cookie = new Cookie(cookieName, cookieValue2);
                    cookie.setPath("/foo/bar");
                    response.addCookie(cookie);
                }
                else
                {
                    Cookie[] cookies = Optional.ofNullable(request.getCookies()).orElse(EMPTY_COOKIES);
                    switch (target)
                    {
                        case "/":
                            assertEquals(0, cookies.length, target);
                            break;
                        case "/foo":
                            assertEquals(1, cookies.length, target);
                            Cookie cookie = cookies[0];
                            assertEquals(cookieName, cookie.getName(), target);
                            assertEquals(cookieValue1, cookie.getValue(), target);
                            break;
                        case "/foo/bar":
                            assertEquals(2, cookies.length, target);
                            Cookie cookie1 = cookies[0];
                            Cookie cookie2 = cookies[1];
                            assertEquals(cookieName, cookie1.getName(), target);
                            assertEquals(cookieName, cookie2.getName(), target);
                            Set<String> values = new HashSet<>();
                            values.add(cookie1.getValue());
                            values.add(cookie2.getValue());
                            assertThat(target, values, containsInAnyOrder(cookieValue1, cookieValue2));
                            break;
                        default:
                            fail("Unrecognized Target: " + target);
                    }
                }
            }
        });

        ContentResponse response = send(client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .path("/foo")
                .header(headerName, "0")
                .timeout(5, TimeUnit.SECONDS));
        assertEquals(HttpStatus.OK_200, response.getStatus());

        Arrays.asList("/", "/foo", "/foo/bar").forEach(path ->
        {
            ContentResponse r = send(client.newRequest("localhost", connector.getLocalPort())
                    .scheme(scenario.getScheme())
                    .path(path)
                    .header(headerName, "1")
                    .timeout(5, TimeUnit.SECONDS));
            assertEquals(HttpStatus.OK_200, r.getStatus());
        });
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_CookiePathWithTrailingSlash(Scenario scenario) throws Exception
    {
        String headerName = "X-Request";
        String cookieName = "a";
        String cookieValue = "1";
        start(scenario, new EmptyServerHandler()
        {
            @Override
            protected void service(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                int r = request.getIntHeader(headerName);
                if ("/foo/bar".equals(target) && r == 0)
                {
                    Cookie cookie = new Cookie(cookieName, cookieValue);
                    cookie.setPath("/foo/");
                    response.addCookie(cookie);
                }
                else
                {
                    Cookie[] cookies = Optional.ofNullable(request.getCookies()).orElse(EMPTY_COOKIES);
                    switch (target)
                    {
                        case "/":
                        case "/foo":
                        case "/foobar":
                            assertEquals(0, cookies.length, target);
                            break;
                        case "/foo/":
                        case "/foo/bar":
                            assertEquals(1, cookies.length, target);
                            Cookie cookie = cookies[0];
                            assertEquals(cookieName, cookie.getName(), target);
                            assertEquals(cookieValue, cookie.getValue(), target);
                            break;
                        default:
                            fail("Unrecognized Target: " + target);
                    }
                }
            }
        });

        ContentResponse response = send(client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .path("/foo/bar")
                .header(headerName, "0")
                .timeout(5, TimeUnit.SECONDS));
        assertEquals(HttpStatus.OK_200, response.getStatus());

        Arrays.asList("/", "/foo", "/foo/", "/foobar", "/foo/bar").forEach(path ->
        {
            ContentResponse r = send(client.newRequest("localhost", connector.getLocalPort())
                    .scheme(scenario.getScheme())
                    .path(path)
                    .header(headerName, "1")
                    .timeout(5, TimeUnit.SECONDS));
            assertEquals(HttpStatus.OK_200, r.getStatus());
        });
    }

    private ContentResponse send(org.eclipse.jetty.client.api.Request request)
    {
        try
        {
            return request.send();
        }
        catch (RuntimeException x)
        {
            throw x;
        }
        catch (Exception x)
        {
            throw new RuntimeException(x);
        }
    }
}
