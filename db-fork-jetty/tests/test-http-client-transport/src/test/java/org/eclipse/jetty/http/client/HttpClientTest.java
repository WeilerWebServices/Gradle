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

package org.eclipse.jetty.http.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.FutureResponseListener;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http2.FlowControlStrategy;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

public class HttpClientTest extends AbstractTest<TransportScenario>
{
    @Override
    public void init(Transport transport) throws IOException
    {
        setScenario(new TransportScenario(transport));
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testRequestWithoutResponseContent(Transport transport) throws Exception
    {
        init(transport);
        final int status = HttpStatus.NO_CONTENT_204;
        scenario.start(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            {
                baseRequest.setHandled(true);
                response.setStatus(status);
            }
        });

        ContentResponse response = scenario.client.newRequest(scenario.newURI())
                .timeout(5, TimeUnit.SECONDS)
                .send();

        assertEquals(status, response.getStatus());
        assertEquals(0, response.getContent().length);
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testRequestWithSmallResponseContent(Transport transport) throws Exception
    {
        init(transport);
        testRequestWithResponseContent(1024);
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testRequestWithLargeResponseContent(Transport transport) throws Exception
    {
        init(transport);
        testRequestWithResponseContent(1024 * 1024);
    }

    private void testRequestWithResponseContent(int length) throws Exception
    {
        final byte[] bytes = new byte[length];
        new Random().nextBytes(bytes);
        scenario.start(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
            {
                baseRequest.setHandled(true);
                response.setContentLength(length);
                response.getOutputStream().write(bytes);
            }
        });

        org.eclipse.jetty.client.api.Request request = scenario.client.newRequest(scenario.newURI());
        FutureResponseListener listener = new FutureResponseListener(request, length);
        request.timeout(10, TimeUnit.SECONDS).send(listener);
        ContentResponse response = listener.get();

        assertEquals(200, response.getStatus());
        assertArrayEquals(bytes, response.getContent());
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testRequestWithSmallResponseContentChunked(Transport transport) throws Exception
    {
        init(transport);
        testRequestWithResponseContentChunked(512);
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testRequestWithLargeResponseContentChunked(Transport transport) throws Exception
    {
        init(transport);
        testRequestWithResponseContentChunked(512 * 512);
    }

    private void testRequestWithResponseContentChunked(int length) throws Exception
    {
        final byte[] chunk1 = new byte[length];
        final byte[] chunk2 = new byte[length];
        Random random = new Random();
        random.nextBytes(chunk1);
        random.nextBytes(chunk2);
        byte[] bytes = new byte[chunk1.length + chunk2.length];
        System.arraycopy(chunk1, 0, bytes, 0, chunk1.length);
        System.arraycopy(chunk2, 0, bytes, chunk1.length, chunk2.length);
        scenario.start(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
            {
                baseRequest.setHandled(true);
                ServletOutputStream output = response.getOutputStream();
                output.write(chunk1);
                output.flush();
                output.write(chunk2);
            }
        });

        org.eclipse.jetty.client.api.Request request = scenario.client.newRequest(scenario.newURI());
        FutureResponseListener listener = new FutureResponseListener(request, 2 * length);
        request.timeout(10, TimeUnit.SECONDS).send(listener);
        ContentResponse response = listener.get();

        assertEquals(200, response.getStatus());
        assertArrayEquals(bytes, response.getContent());
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testUploadZeroLengthWithoutResponseContent(Transport transport) throws Exception
    {
        init(transport);
        testUploadWithoutResponseContent(0);
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testUploadSmallWithoutResponseContent(Transport transport) throws Exception
    {
        init(transport);
        testUploadWithoutResponseContent(1024);
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testUploadLargeWithoutResponseContent(Transport transport) throws Exception
    {
        init(transport);
        testUploadWithoutResponseContent(1024 * 1024);
    }

    private void testUploadWithoutResponseContent(int length) throws Exception
    {
        final byte[] bytes = new byte[length];
        new Random().nextBytes(bytes);
        scenario.start(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
            {
                baseRequest.setHandled(true);
                ServletInputStream input = request.getInputStream();
                for (byte b : bytes)
                    assertEquals(b & 0xFF, input.read());
                assertEquals(-1, input.read());
            }
        });

        ContentResponse response = scenario.client.newRequest(scenario.newURI())
                .method(HttpMethod.POST)
                .content(new BytesContentProvider(bytes))
                .timeout(15, TimeUnit.SECONDS)
                .send();

        assertEquals(200, response.getStatus());
        assertEquals(0, response.getContent().length);
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testClientManyWritesSlowServer(Transport transport) throws Exception
    {
        init(transport);
        scenario.start(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
            {
                baseRequest.setHandled(true);

                long sleep = 1024;
                long total = 0;
                ServletInputStream input = request.getInputStream();
                byte[] buffer = new byte[1024];
                while (true)
                {
                    int read = input.read(buffer);
                    if (read < 0)
                        break;
                    total += read;
                    if (total >= sleep)
                    {
                        sleep(250);
                        sleep += 256;
                    }
                }

                response.getOutputStream().print(total);
            }
        });

        int chunks = 256;
        int chunkSize = 16;
        byte[][] bytes = IntStream.range(0, chunks).mapToObj(x -> new byte[chunkSize]).toArray(byte[][]::new);
        BytesContentProvider contentProvider = new BytesContentProvider("application/octet-stream", bytes);
        ContentResponse response = scenario.client.newRequest(scenario.newURI())
                .method(HttpMethod.POST)
                .content(contentProvider)
                .timeout(15, TimeUnit.SECONDS)
                .send();

        assertEquals(HttpStatus.OK_200, response.getStatus());
        assertEquals(chunks * chunkSize, Integer.parseInt(response.getContentAsString()));
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testRequestAfterFailedRequest(Transport transport) throws Exception
    {
        init(transport);
        int length = FlowControlStrategy.DEFAULT_WINDOW_SIZE;
        scenario.start(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            {
                try
                {
                    baseRequest.setHandled(true);
                    response.getOutputStream().write(new byte[length]);
                }
                catch(IOException ignored)
                {
                }
            }
        });

        // Make a request with a large enough response buffer.
        org.eclipse.jetty.client.api.Request request = scenario.client.newRequest(scenario.newURI());
        FutureResponseListener listener = new FutureResponseListener(request, length);
        request.send(listener);
        ContentResponse response = listener.get(5, TimeUnit.SECONDS);
        assertEquals(response.getStatus(), 200);

        // Make a request with a small response buffer, should fail.
        try
        {
            request = scenario.client.newRequest(scenario.newURI());
            listener = new FutureResponseListener(request, length / 10);
            request.send(listener);
            listener.get(5, TimeUnit.SECONDS);
            fail("Expected ExecutionException");
        }
        catch (ExecutionException x)
        {
            assertThat(x.getMessage(),containsString("exceeded"));
        }

        // Verify that we can make another request.
        request = scenario.client.newRequest(scenario.newURI());
        listener = new FutureResponseListener(request, length);
        request.send(listener);
        response = listener.get(5, TimeUnit.SECONDS);
        assertEquals(response.getStatus(), 200);
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testClientCannotValidateServerCertificate(Transport transport) throws Exception
    {
        init(transport);
        // Only run this test for transports over TLS.
        Assumptions.assumeTrue(scenario.isTransportSecure());

        scenario.startServer(new EmptyServerHandler());

        // Use a default SslContextFactory, requests should fail because the server certificate is unknown.
        scenario.client = scenario.newHttpClient(scenario.provideClientTransport(), new SslContextFactory());
        QueuedThreadPool clientThreads = new QueuedThreadPool();
        clientThreads.setName("client");
        scenario.client.setExecutor(clientThreads);
        scenario.client.start();

        assertThrows(ExecutionException.class, ()-> {
            scenario.client.newRequest(scenario.newURI())
                    .timeout(5, TimeUnit.SECONDS)
                    .send();
        });
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testOPTIONS(Transport transport) throws Exception
    {
        init(transport);
        scenario.start(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            {
                baseRequest.setHandled(true);
                assertTrue(HttpMethod.OPTIONS.is(request.getMethod()));
                assertEquals("*", target);
                assertEquals("*", request.getPathInfo());
            }
        });

        ContentResponse response = scenario.client.newRequest(scenario.newURI())
                .scheme(scenario.getScheme())
                .method(HttpMethod.OPTIONS)
                .path("*")
                .timeout(5, TimeUnit.SECONDS)
                .send();

        assertEquals(HttpStatus.OK_200, response.getStatus());
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testOPTIONSWithRelativeRedirect(Transport transport) throws Exception
    {
        init(transport);
        scenario.start(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            {
                baseRequest.setHandled(true);
                if ("*".equals(target))
                {
                    // Be nasty and send a relative redirect.
                    // Code 303 will change the method to GET.
                    response.setStatus(HttpStatus.SEE_OTHER_303);
                    response.setHeader("Location", "/");
                }
            }
        });

        ContentResponse response = scenario.client.newRequest(scenario.newURI())
                .scheme(scenario.getScheme())
                .method(HttpMethod.OPTIONS)
                .path("*")
                .timeout(5, TimeUnit.SECONDS)
                .send();

        assertEquals(HttpStatus.OK_200, response.getStatus());
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testDownloadWithInputStreamResponseListener(Transport transport) throws Exception
    {
        init(transport);
        String content = "hello world";
        scenario.start(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
            {
                baseRequest.setHandled(true);
                response.getOutputStream().print(content);
            }
        });

        CountDownLatch latch = new CountDownLatch(1);
        InputStreamResponseListener listener = new InputStreamResponseListener();
        scenario.client.newRequest(scenario.newURI())
                .scheme(scenario.getScheme())
                .onResponseSuccess(response -> latch.countDown())
                .send(listener);
        Response response = listener.get(5, TimeUnit.SECONDS);
        assertEquals(200, response.getStatus());

        // Response cannot succeed until we read the content.
        assertFalse(latch.await(500, TimeUnit.MILLISECONDS));

        InputStream input = listener.getInputStream();
        assertEquals(content, IO.toString(input));

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testConnectionListener(Transport transport) throws Exception
    {
        init(transport);
        scenario.start(new EmptyServerHandler());

        CountDownLatch openLatch = new CountDownLatch(1);
        CountDownLatch closeLatch = new CountDownLatch(1);
        scenario.client.addBean(new org.eclipse.jetty.io.Connection.Listener()
        {
            @Override
            public void onOpened(org.eclipse.jetty.io.Connection connection)
            {
                openLatch.countDown();
            }

            @Override
            public void onClosed(org.eclipse.jetty.io.Connection connection)
            {
                closeLatch.countDown();
            }
        });

        long idleTimeout = 1000;
        scenario.client.setIdleTimeout(idleTimeout);

        ContentResponse response = scenario.client.newRequest(scenario.newURI())
                .scheme(scenario.getScheme())
                .timeout(5, TimeUnit.SECONDS)
                .send();

        assertEquals(HttpStatus.OK_200, response.getStatus());
        assertTrue(openLatch.await(1, TimeUnit.SECONDS));

        Thread.sleep(2 * idleTimeout);
        assertTrue(closeLatch.await(1, TimeUnit.SECONDS));
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testAsyncResponseContentBackPressure(Transport transport) throws Exception
    {
        init(transport);
        scenario.start(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
            {
                baseRequest.setHandled(true);
                // Large write to generate multiple DATA frames.
                response.getOutputStream().write(new byte[256 * 1024]);
            }
        });

        CountDownLatch completeLatch = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger();
        AtomicReference<Callback> callbackRef = new AtomicReference<>();
        AtomicReference<CountDownLatch> latchRef = new AtomicReference<>(new CountDownLatch(1));
        scenario.client.newRequest(scenario.newURI())
                .scheme(scenario.getScheme())
                .onResponseContentAsync((response, content, callback) ->
                {
                    if (counter.incrementAndGet() == 1)
                    {
                        callbackRef.set(callback);
                        latchRef.get().countDown();
                    }
                    else
                    {
                        callback.succeeded();
                    }
                })
                .send(result -> completeLatch.countDown());

        assertTrue(latchRef.get().await(5, TimeUnit.SECONDS));
        // Wait some time to verify that back pressure is applied correctly.
        Thread.sleep(1000);
        assertEquals(1, counter.get());
        callbackRef.get().succeeded();

        assertTrue(completeLatch.await(5, TimeUnit.SECONDS));
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testResponseWithContentCompleteListenerInvokedOnce(Transport transport) throws Exception
    {
        init(transport);
        scenario.start(new EmptyServerHandler()
        {
            @Override
            protected void service(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
            {
                response.getWriter().write("Jetty");
            }
        });

        AtomicInteger completes = new AtomicInteger();
        scenario.client.newRequest(scenario.newURI())
                .send(result -> completes.incrementAndGet());

        sleep(1000);

        assertEquals(1, completes.get());
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testHEADResponds200(Transport transport) throws Exception
    {
        init(transport);
        testHEAD(scenario.servletPath, HttpStatus.OK_200);
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testHEADResponds404(Transport transport) throws Exception
    {
        init(transport);
        testHEAD("/notMapped", HttpStatus.NOT_FOUND_404);
    }

    private void testHEAD(String path, int status) throws Exception
    {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        scenario.start(new HttpServlet()
        {
            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
            {
                response.getOutputStream().write(data);
            }
        });

        ContentResponse response = scenario.client.newRequest(scenario.newURI())
                .method(HttpMethod.HEAD)
                .path(path)
                .send();

        assertEquals(status, response.getStatus());
        assertEquals(0, response.getContent().length);
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testHEADWithAcceptHeaderAndSendError(Transport transport) throws Exception
    {
        init(transport);
        int status = HttpStatus.BAD_REQUEST_400;
        scenario.start(new HttpServlet()
        {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
            {
                resp.sendError(status);
            }
        });

        ContentResponse response = scenario.client.newRequest(scenario.newURI())
                .method(HttpMethod.HEAD)
                .path(scenario.servletPath)
                .header(HttpHeader.ACCEPT, "*/*")
                .send();

        assertEquals(status, response.getStatus());
        assertEquals(0, response.getContent().length);
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testHEADWithContentLengthGreaterThanMaxBufferingCapacity(Transport transport) throws Exception
    {
        int length = 1024;
        init(transport);
        scenario.start(new HttpServlet()
        {
            @Override
            protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
            {
                response.setContentLength(length);
                response.getOutputStream().write(new byte[length]);
            }
        });

        org.eclipse.jetty.client.api.Request request = scenario.client
                .newRequest(scenario.newURI())
                .method(HttpMethod.HEAD)
                .path(scenario.servletPath);
        FutureResponseListener listener = new FutureResponseListener(request, length / 2);
        request.send(listener);
        ContentResponse response = listener.get(5, TimeUnit.SECONDS);

        assertEquals(HttpStatus.OK_200, response.getStatus());
        assertEquals(0, response.getContent().length);
    }

    private void sleep(long time) throws IOException
    {
        try
        {
            Thread.sleep(time);
        }
        catch (InterruptedException x)
        {
            throw new InterruptedIOException();
        }
    }
}
