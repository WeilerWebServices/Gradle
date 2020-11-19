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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLEngine;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Connection;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Destination;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ssl.SslClientConnectionFactory;
import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.FuturePromise;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static org.eclipse.jetty.http.client.Transport.UNIX_SOCKET;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class HttpClientTimeoutTest extends AbstractTest<TransportScenario>
{
    @Override
    public void init(Transport transport) throws IOException
    {
        setScenario(new TransportScenario(transport));
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testTimeoutOnFuture(Transport transport) throws Exception
    {
        init(transport);
        long timeout = 1000;
        scenario.start(new TimeoutHandler(2 * timeout));

        assertThrows(TimeoutException.class, () ->
        {
            scenario.client.newRequest(scenario.newURI())
                    .timeout(timeout, TimeUnit.MILLISECONDS)
                    .send();
        });
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testTimeoutOnListener(Transport transport) throws Exception
    {
        init(transport);
        long timeout = 1000;
        scenario.start(new TimeoutHandler(2 * timeout));

        final CountDownLatch latch = new CountDownLatch(1);
        Request request = scenario.client.newRequest(scenario.newURI())
                .timeout(timeout, TimeUnit.MILLISECONDS);
        request.send(result ->
        {
            assertTrue(result.isFailed());
            latch.countDown();
        });
        assertTrue(latch.await(3 * timeout, TimeUnit.MILLISECONDS));
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testTimeoutOnQueuedRequest(Transport transport) throws Exception
    {
        init(transport);
        long timeout = 1000;
        scenario.start(new TimeoutHandler(3 * timeout));

        // Only one connection so requests get queued
        scenario.client.setMaxConnectionsPerDestination(1);

        // The first request has a long timeout
        final CountDownLatch firstLatch = new CountDownLatch(1);
        Request request = scenario.client.newRequest(scenario.newURI())
                .timeout(4 * timeout, TimeUnit.MILLISECONDS);
        request.send(result ->
        {
            assertFalse(result.isFailed());
            firstLatch.countDown();
        });

        // Second request has a short timeout and should fail in the queue
        final CountDownLatch secondLatch = new CountDownLatch(1);
        request = scenario.client.newRequest(scenario.newURI())
                .timeout(timeout, TimeUnit.MILLISECONDS);
        request.send(result ->
        {
            assertTrue(result.isFailed());
            secondLatch.countDown();
        });

        assertTrue(secondLatch.await(2 * timeout, TimeUnit.MILLISECONDS));
        // The second request must fail before the first request has completed
        assertTrue(firstLatch.getCount() > 0);
        assertTrue(firstLatch.await(5 * timeout, TimeUnit.MILLISECONDS));
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testTimeoutIsCancelledOnSuccess(Transport transport) throws Exception
    {
        init(transport);
        long timeout = 1000;
        scenario.start(new TimeoutHandler(timeout));

        final CountDownLatch latch = new CountDownLatch(1);
        final byte[] content = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        Request request = scenario.client.newRequest(scenario.newURI())
                .content(new InputStreamContentProvider(new ByteArrayInputStream(content)))
                .timeout(2 * timeout, TimeUnit.MILLISECONDS);
        request.send(new BufferingResponseListener()
        {
            @Override
            public void onComplete(Result result)
            {
                assertFalse(result.isFailed());
                assertArrayEquals(content, getContent());
                latch.countDown();
            }
        });

        assertTrue(latch.await(3 * timeout, TimeUnit.MILLISECONDS));

        TimeUnit.MILLISECONDS.sleep(2 * timeout);

        assertNull(request.getAbortCause());
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testTimeoutOnListenerWithExplicitConnection(Transport transport) throws Exception
    {
        assumeRealNetwork(transport);
        init(transport);

        long timeout = 1000;
        scenario.start(new TimeoutHandler(2 * timeout));

        final CountDownLatch latch = new CountDownLatch(1);
        Destination destination = scenario.client.getDestination(scenario.getScheme(), "localhost", scenario.getNetworkConnectorLocalPortInt().get());
        FuturePromise<Connection> futureConnection = new FuturePromise<>();
        destination.newConnection(futureConnection);
        try (Connection connection = futureConnection.get(5, TimeUnit.SECONDS))
        {
            Request request = scenario.client.newRequest(scenario.newURI())
                    .timeout(timeout, TimeUnit.MILLISECONDS);
            connection.send(request, result ->
            {
                assertTrue(result.isFailed());
                latch.countDown();
            });

            assertTrue(latch.await(3 * timeout, TimeUnit.MILLISECONDS));
        }
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testTimeoutIsCancelledOnSuccessWithExplicitConnection(Transport transport) throws Exception
    {
        assumeRealNetwork(transport);
        init(transport);

        long timeout = 1000;
        scenario.start(new TimeoutHandler(timeout));

        final CountDownLatch latch = new CountDownLatch(1);
        Destination destination = scenario.client.getDestination(scenario.getScheme(), "localhost", scenario.getNetworkConnectorLocalPortInt().get());
        FuturePromise<Connection> futureConnection = new FuturePromise<>();
        destination.newConnection(futureConnection);
        try (Connection connection = futureConnection.get(5, TimeUnit.SECONDS))
        {
            Request request = scenario.client.newRequest(scenario.newURI())
                    .timeout(2 * timeout, TimeUnit.MILLISECONDS);
            connection.send(request, result ->
            {
                Response response = result.getResponse();
                assertEquals(200, response.getStatus());
                assertFalse(result.isFailed());
                latch.countDown();
            });

            assertTrue(latch.await(3 * timeout, TimeUnit.MILLISECONDS));

            TimeUnit.MILLISECONDS.sleep(2 * timeout);

            assertNull(request.getAbortCause());
        }
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testIdleTimeout(Transport transport) throws Exception
    {
        init(transport);
        long timeout = 1000;
        scenario.startServer(new TimeoutHandler(2 * timeout));

        AtomicBoolean sslIdle = new AtomicBoolean();
        SslContextFactory sslContextFactory = scenario.newSslContextFactory();
        sslContextFactory.setEndpointIdentificationAlgorithm(null);
        scenario.client = new HttpClient(scenario.provideClientTransport(), sslContextFactory)
        {
            @Override
            public ClientConnectionFactory newSslClientConnectionFactory(ClientConnectionFactory connectionFactory)
            {
                return new SslClientConnectionFactory(getSslContextFactory(), getByteBufferPool(), getExecutor(), connectionFactory)
                {
                    @Override
                    protected SslConnection newSslConnection(ByteBufferPool byteBufferPool, Executor executor, EndPoint endPoint, SSLEngine engine)
                    {
                        return new SslConnection(byteBufferPool, executor, endPoint, engine)
                        {
                            @Override
                            protected boolean onReadTimeout(Throwable timeout)
                            {
                                sslIdle.set(true);
                                return super.onReadTimeout(timeout);
                            }
                        };
                    }
                };
            }
        };
        scenario.client.setIdleTimeout(timeout);
        scenario.client.start();

        assertThrows(TimeoutException.class, () ->
        {
            scenario.client.newRequest(scenario.newURI())
                    .send();
        });
        assertFalse(sslIdle.get());
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testBlockingConnectTimeoutFailsRequest(Transport transport) throws Exception
    {
        assumeRealNetwork(transport);
        init(transport);
        testConnectTimeoutFailsRequest(true);
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testNonBlockingConnectTimeoutFailsRequest(Transport transport) throws Exception
    {
        assumeRealNetwork(transport);
        init(transport);
        testConnectTimeoutFailsRequest(false);
    }

    private void testConnectTimeoutFailsRequest(boolean blocking) throws Exception
    {
        String host = "10.255.255.1";
        int port = 80;
        int connectTimeout = 1000;
        assumeConnectTimeout(host, port, connectTimeout);

        scenario.start(new EmptyServerHandler());
        HttpClient client = scenario.client;
        client.stop();
        client.setConnectTimeout(connectTimeout);
        client.setConnectBlocking(blocking);
        client.start();

        final CountDownLatch latch = new CountDownLatch(1);
        Request request = client.newRequest(host, port);
        request.scheme(scenario.getScheme())
                .send(result ->
                {
                    if (result.isFailed())
                        latch.countDown();
                });

        assertTrue(latch.await(2 * connectTimeout, TimeUnit.MILLISECONDS));
        assertNotNull(request.getAbortCause());
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testConnectTimeoutIsCancelledByShorterRequestTimeout(Transport transport) throws Exception
    {
        assumeRealNetwork(transport);
        init(transport);

        String host = "10.255.255.1";
        int port = 80;
        int connectTimeout = 2000;
        assumeConnectTimeout(host, port, connectTimeout);

        scenario.start(new EmptyServerHandler());
        HttpClient client = scenario.client;
        client.stop();
        client.setConnectTimeout(connectTimeout);
        client.start();

        final AtomicInteger completes = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(2);
        Request request = client.newRequest(host, port);
        request.scheme(scenario.getScheme())
                .timeout(connectTimeout / 2, TimeUnit.MILLISECONDS)
                .send(result ->
                {
                    completes.incrementAndGet();
                    latch.countDown();
                });

        assertFalse(latch.await(2 * connectTimeout, TimeUnit.MILLISECONDS));
        assertEquals(1, completes.get());
        assertNotNull(request.getAbortCause());
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void retryAfterConnectTimeout(Transport transport) throws Exception
    {
        assumeRealNetwork(transport);
        init(transport);

        final String host = "10.255.255.1";
        final int port = 80;
        int connectTimeout = 1000;
        assumeConnectTimeout(host, port, connectTimeout);

        scenario.start(new EmptyServerHandler());
        HttpClient client = scenario.client;
        client.stop();
        client.setConnectTimeout(connectTimeout);
        client.start();

        final CountDownLatch latch = new CountDownLatch(1);
        Request request = client.newRequest(host, port);
        request.scheme(scenario.getScheme())
                .send(result ->
                {
                    if (result.isFailed())
                    {
                        // Retry
                        client.newRequest(host, port)
                                .scheme(scenario.getScheme())
                                .send(retryResult ->
                                {
                                    if (retryResult.isFailed())
                                        latch.countDown();
                                });
                    }
                });

        assertTrue(latch.await(333 * connectTimeout, TimeUnit.MILLISECONDS));
        assertNotNull(request.getAbortCause());
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testVeryShortTimeout(Transport transport) throws Exception
    {
        init(transport);
        scenario.start(new EmptyServerHandler());

        final CountDownLatch latch = new CountDownLatch(1);
        scenario.client.newRequest(scenario.newURI())
                .timeout(1, TimeUnit.MILLISECONDS) // Very short timeout
                .send(result -> latch.countDown());

        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testTimeoutCancelledWhenSendingThrowsException(Transport transport) throws Exception
    {
        assumeRealNetwork(transport);
        init(transport);

        scenario.start(new EmptyServerHandler());

        long timeout = 1000;
        String uri = "badscheme://0.0.0.1";
        if (scenario.getNetworkConnectorLocalPort().isPresent())
            uri += ":" + scenario.getNetworkConnectorLocalPort().get();
        Request request = scenario.client.newRequest(uri);

        // TODO: assert a more specific Throwable
        assertThrows(Exception.class, () ->
        {
            request.timeout(timeout, TimeUnit.MILLISECONDS)
                    .send(result ->
                    {
                    });
        });

        Thread.sleep(2 * timeout);

        // If the task was not cancelled, it aborted the request.
        assertNull(request.getAbortCause());
    }

    private void assumeRealNetwork(Transport transport)
    {
        Assumptions.assumeTrue(transport != UNIX_SOCKET);
    }

    @ParameterizedTest
    @ArgumentsSource(TransportProvider.class)
    public void testFirstRequestTimeoutAfterSecondRequestCompletes(Transport transport) throws Exception
    {
        init(transport);
        long timeout = 2000;
        scenario.start(new EmptyServerHandler()
        {
            @Override
            protected void service(String target, org.eclipse.jetty.server.Request jettyRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
            {
                if (request.getRequestURI().startsWith("/one"))
                {
                    try
                    {
                        Thread.sleep(3 * timeout);
                    }
                    catch (InterruptedException x)
                    {
                        throw new InterruptedIOException();
                    }
                }
            }
        });

        CountDownLatch latch = new CountDownLatch(1);
        scenario.client.newRequest(scenario.newURI())
                .path("/one")
                .timeout(2 * timeout, TimeUnit.MILLISECONDS)
                .send(result ->
                {
                    if (result.isFailed() && result.getFailure() instanceof TimeoutException)
                        latch.countDown();
                });

        ContentResponse response = scenario.client.newRequest(scenario.newURI())
                .path("/two")
                .timeout(timeout, TimeUnit.MILLISECONDS)
                .send();

        assertEquals(HttpStatus.OK_200, response.getStatus());
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    private void assumeConnectTimeout(String host, int port, int connectTimeout)
    {
        try (Socket socket = new Socket())
        {
            // Try to connect to a private address in the 10.x.y.z range.
            // These addresses are usually not routed, so an attempt to
            // connect to them will hang the connection attempt, which is
            // what we want to simulate in this test.
            socket.connect(new InetSocketAddress(host, port), connectTimeout);
            // Abort the test if we can connect.
            fail("Error: Should not have been able to connect to " + host + ":" + port);
        }
        catch (SocketTimeoutException x)
        {
            // Expected timeout during connect, continue the test.
            return;
        }
        catch (Throwable x)
        {
            // Abort if any other exception happens.
            fail(x);
        }
    }

    private class TimeoutHandler extends AbstractHandler
    {
        private final long timeout;

        public TimeoutHandler(long timeout)
        {
            this.timeout = timeout;
        }

        @Override
        public void handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
        {
            baseRequest.setHandled(true);
            try
            {
                TimeUnit.MILLISECONDS.sleep(timeout);
                IO.copy(request.getInputStream(), response.getOutputStream());
            }
            catch (InterruptedException x)
            {
                throw new ServletException(x);
            }
        }
    }
}
