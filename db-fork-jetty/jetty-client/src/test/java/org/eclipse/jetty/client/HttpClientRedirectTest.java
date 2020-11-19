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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.UnresolvedAddressException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.ByteBufferContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.toolchain.test.IO;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

public class HttpClientRedirectTest extends AbstractHttpClientServerTest
{
    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_303(Scenario scenario) throws Exception
    {
        start(scenario, new RedirectHandler());

        Response response = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .path("/303/localhost/done")
                .timeout(5, TimeUnit.SECONDS)
                .send();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertFalse(response.getHeaders().containsKey(HttpHeader.LOCATION.asString()));
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_303_302(Scenario scenario) throws Exception
    {
        start(scenario, new RedirectHandler());

        Response response = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .path("/303/localhost/302/localhost/done")
                .timeout(5, TimeUnit.SECONDS)
                .send();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertFalse(response.getHeaders().containsKey(HttpHeader.LOCATION.asString()));
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_303_302_OnDifferentDestinations(Scenario scenario) throws Exception
    {
        start(scenario, new RedirectHandler());

        Response response = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .path("/303/127.0.0.1/302/localhost/done")
                .timeout(5, TimeUnit.SECONDS)
                .send();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertFalse(response.getHeaders().containsKey(HttpHeader.LOCATION.asString()));
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_301(Scenario scenario) throws Exception
    {
        start(scenario, new RedirectHandler());

        Response response = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .method(HttpMethod.HEAD)
                .path("/301/localhost/done")
                .timeout(5, TimeUnit.SECONDS)
                .send();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertFalse(response.getHeaders().containsKey(HttpHeader.LOCATION.asString()));
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_301_WithWrongMethod(Scenario scenario) throws Exception
    {
        start(scenario, new RedirectHandler());

        ExecutionException x = assertThrows(ExecutionException.class, ()->{
            client.newRequest("localhost", connector.getLocalPort())
                    .scheme(scenario.getScheme())
                    .method(HttpMethod.DELETE)
                    .path("/301/localhost/done")
                    .timeout(5, TimeUnit.SECONDS)
                    .send();
        });
        HttpResponseException xx = (HttpResponseException)x.getCause();
        Response response = xx.getResponse();
        assertNotNull(response);
        assertEquals(301, response.getStatus());
        assertTrue(response.getHeaders().containsKey(HttpHeader.LOCATION.asString()));
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_307_WithRequestContent(Scenario scenario) throws Exception
    {
        start(scenario, new RedirectHandler());

        byte[] data = new byte[]{0, 1, 2, 3, 4, 5, 6, 7};
        ContentResponse response = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .method(HttpMethod.POST)
                .path("/307/localhost/done")
                .content(new ByteBufferContentProvider(ByteBuffer.wrap(data)))
                .timeout(5, TimeUnit.SECONDS)
                .send();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertFalse(response.getHeaders().containsKey(HttpHeader.LOCATION.asString()));
        assertArrayEquals(data, response.getContent());
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void testMaxRedirections(Scenario scenario) throws Exception
    {
        start(scenario, new RedirectHandler());
        client.setMaxRedirects(1);

        ExecutionException x = assertThrows(ExecutionException.class, ()->{
            client.newRequest("localhost", connector.getLocalPort())
                    .scheme(scenario.getScheme())
                    .path("/303/localhost/302/localhost/done")
                    .timeout(5, TimeUnit.SECONDS)
                    .send();
        });
        HttpResponseException xx = (HttpResponseException)x.getCause();
        Response response = xx.getResponse();
        assertNotNull(response);
        assertEquals(302, response.getStatus());
        assertTrue(response.getHeaders().containsKey(HttpHeader.LOCATION.asString()));
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_303_WithConnectionClose_WithBigRequest(Scenario scenario) throws Exception
    {
        start(scenario, new RedirectHandler());

        Response response = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .path("/303/localhost/done?close=true")
                .timeout(5, TimeUnit.SECONDS)
                .send();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertFalse(response.getHeaders().containsKey(HttpHeader.LOCATION.asString()));
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void testDontFollowRedirects(Scenario scenario) throws Exception
    {
        start(scenario, new RedirectHandler());

        Response response = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .followRedirects(false)
                .path("/303/localhost/done?close=true")
                .timeout(5, TimeUnit.SECONDS)
                .send();
        assertNotNull(response);
        assertEquals(303, response.getStatus());
        assertTrue(response.getHeaders().containsKey(HttpHeader.LOCATION.asString()));
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void testRelativeLocation(Scenario scenario) throws Exception
    {
        start(scenario, new RedirectHandler());

        Response response = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .path("/303/localhost/done?relative=true")
                .timeout(5, TimeUnit.SECONDS)
                .send();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertFalse(response.getHeaders().containsKey(HttpHeader.LOCATION.asString()));
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void testAbsoluteURIPathWithSpaces(Scenario scenario) throws Exception
    {
        start(scenario, new RedirectHandler());

        Response response = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .path("/303/localhost/a+space?decode=true")
                .timeout(5, TimeUnit.SECONDS)
                .send();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertFalse(response.getHeaders().containsKey(HttpHeader.LOCATION.asString()));
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void testRelativeURIPathWithSpaces(Scenario scenario) throws Exception
    {
        start(scenario, new RedirectHandler());

        Response response = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .path("/303/localhost/a+space?relative=true&decode=true")
                .timeout(5, TimeUnit.SECONDS)
                .send();
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertFalse(response.getHeaders().containsKey(HttpHeader.LOCATION.asString()));
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void testRedirectWithWrongScheme(Scenario scenario) throws Exception
    {
        start(scenario, new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                baseRequest.setHandled(true);
                response.setStatus(303);
                response.setHeader("Location", "ssh://localhost:" + connector.getLocalPort() + "/path");
            }
        });

        final CountDownLatch latch = new CountDownLatch(1);
        client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .path("/path")
                .timeout(5, TimeUnit.SECONDS)
                .send(result ->
                {
                    assertTrue(result.isFailed());
                    latch.countDown();
                });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    @Disabled
    public void testRedirectFailed(Scenario scenario) throws Exception
    {
        // TODO this test is failing with timout after an ISP upgrade??  DNS dependent?
        start(scenario, new RedirectHandler());

        try
        {
            client.newRequest("localhost", connector.getLocalPort())
                    .scheme(scenario.getScheme())
                    .path("/303/doesNotExist/done")
                    .timeout(5, TimeUnit.SECONDS)
                    .send();
        }
        catch (ExecutionException x)
        {
            assertThat(x.getCause(), Matchers.instanceOf(UnresolvedAddressException.class));
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_HEAD_301(Scenario scenario) throws Exception
    {
        testSameMethodRedirect(scenario, HttpMethod.HEAD, HttpStatus.MOVED_PERMANENTLY_301);
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_POST_301(Scenario scenario) throws Exception
    {
        testGETRedirect(scenario, HttpMethod.POST, HttpStatus.MOVED_PERMANENTLY_301);
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_PUT_301(Scenario scenario) throws Exception
    {
        testSameMethodRedirect(scenario, HttpMethod.PUT, HttpStatus.MOVED_PERMANENTLY_301);
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_HEAD_302(Scenario scenario) throws Exception
    {
        testSameMethodRedirect(scenario, HttpMethod.HEAD, HttpStatus.FOUND_302);
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_POST_302(Scenario scenario) throws Exception
    {
        testGETRedirect(scenario, HttpMethod.POST, HttpStatus.FOUND_302);
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_PUT_302(Scenario scenario) throws Exception
    {
        testSameMethodRedirect(scenario, HttpMethod.PUT, HttpStatus.FOUND_302);
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_HEAD_303(Scenario scenario) throws Exception
    {
        testSameMethodRedirect(scenario, HttpMethod.HEAD, HttpStatus.SEE_OTHER_303);
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_POST_303(Scenario scenario) throws Exception
    {
        testGETRedirect(scenario, HttpMethod.POST, HttpStatus.SEE_OTHER_303);
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_PUT_303(Scenario scenario) throws Exception
    {
        testGETRedirect(scenario, HttpMethod.PUT, HttpStatus.SEE_OTHER_303);
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_HEAD_307(Scenario scenario) throws Exception
    {
        testSameMethodRedirect(scenario, HttpMethod.HEAD, HttpStatus.TEMPORARY_REDIRECT_307);
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_POST_307(Scenario scenario) throws Exception
    {
        testSameMethodRedirect(scenario, HttpMethod.POST, HttpStatus.TEMPORARY_REDIRECT_307);
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void test_PUT_307(Scenario scenario) throws Exception
    {
        testSameMethodRedirect(scenario, HttpMethod.PUT, HttpStatus.TEMPORARY_REDIRECT_307);
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void testHttpRedirector(Scenario scenario) throws Exception
    {
        start(scenario, new RedirectHandler());
        final HttpRedirector redirector = new HttpRedirector(client);

        org.eclipse.jetty.client.api.Request request1 = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .path("/303/localhost/302/localhost/done")
                .timeout(5, TimeUnit.SECONDS)
                .followRedirects(false);
        ContentResponse response1 = request1.send();

        assertEquals(303, response1.getStatus());
        assertTrue(redirector.isRedirect(response1));

        Result result = redirector.redirect(request1, response1);
        org.eclipse.jetty.client.api.Request request2 = result.getRequest();
        Response response2 = result.getResponse();

        assertEquals(302, response2.getStatus());
        assertTrue(redirector.isRedirect(response2));

        final CountDownLatch latch = new CountDownLatch(1);
        redirector.redirect(request2, response2, r ->
        {
            Response response3 = r.getResponse();
            assertEquals(200, response3.getStatus());
            assertFalse(redirector.isRedirect(response3));
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @ParameterizedTest
    @ArgumentsSource(ScenarioProvider.class)
    public void testRedirectWithCorruptedBody(Scenario scenario) throws Exception
    {
        byte[] bytes = "ok".getBytes(StandardCharsets.UTF_8);
        start(scenario, new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                baseRequest.setHandled(true);
                if (target.startsWith("/redirect"))
                {
                    response.setStatus(HttpStatus.SEE_OTHER_303);
                    response.setHeader(HttpHeader.LOCATION.asString(), scenario.getScheme() + "://localhost:" + connector.getLocalPort() + "/ok");
                    // Say that we send gzipped content, but actually don't.
                    response.setHeader(HttpHeader.CONTENT_ENCODING.asString(), "gzip");
                    response.getOutputStream().write("redirect".getBytes(StandardCharsets.UTF_8));
                }
                else
                {
                    response.setStatus(HttpStatus.OK_200);
                    response.getOutputStream().write(bytes);
                }
            }
        });

        ContentResponse response = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .path("/redirect")
                .timeout(5, TimeUnit.SECONDS)
                .send();

        assertEquals(200, response.getStatus());
        assertArrayEquals(bytes, response.getContent());
    }

    private void testSameMethodRedirect(final Scenario scenario, final HttpMethod method, int redirectCode) throws Exception
    {
        testMethodRedirect(scenario, method, method, redirectCode);
    }

    private void testGETRedirect(final Scenario scenario, final HttpMethod method, int redirectCode) throws Exception
    {
        testMethodRedirect(scenario, method, HttpMethod.GET, redirectCode);
    }

    private void testMethodRedirect(final Scenario scenario, final HttpMethod requestMethod, final HttpMethod redirectMethod, int redirectCode) throws Exception
    {
        start(scenario, new RedirectHandler());

        final AtomicInteger passes = new AtomicInteger();
        client.getRequestListeners().add(new org.eclipse.jetty.client.api.Request.Listener.Adapter()
        {
            @Override
            public void onBegin(org.eclipse.jetty.client.api.Request request)
            {
                int pass = passes.incrementAndGet();
                if (pass == 1)
                {
                    if (!requestMethod.is(request.getMethod()))
                        request.abort(new Exception());
                }
                else if (pass == 2)
                {
                    if (!redirectMethod.is(request.getMethod()))
                        request.abort(new Exception());
                }
                else
                {
                    request.abort(new Exception());
                }
            }
        });

        ContentResponse response = client.newRequest("localhost", connector.getLocalPort())
                .scheme(scenario.getScheme())
                .method(requestMethod)
                .path("/" + redirectCode + "/localhost/done")
                .timeout(5, TimeUnit.SECONDS)
                .send();

        assertEquals(200, response.getStatus());
    }

    private class RedirectHandler extends AbstractHandler
    {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
        {
            try
            {
                String[] paths = target.split("/", 4);

                int status = Integer.parseInt(paths[1]);
                response.setStatus(status);

                String host = paths[2];
                String path = paths[3];
                boolean relative = Boolean.parseBoolean(request.getParameter("relative"));
                String location = relative ? "" : request.getScheme() + "://" + host + ":" + request.getServerPort();
                location += "/" + path;

                if (Boolean.parseBoolean(request.getParameter("decode")))
                    location = URLDecoder.decode(location, "UTF-8");

                response.setHeader("Location", location);

                if (Boolean.parseBoolean(request.getParameter("close")))
                    response.setHeader("Connection", "close");
            }
            catch (NumberFormatException x)
            {
                response.setStatus(200);
                // Echo content back
                IO.copy(request.getInputStream(), response.getOutputStream());
            }
            finally
            {
                baseRequest.setHandled(true);
            }
        }
    }
}
