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

package org.eclipse.jetty.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OptionalSslConnectionTest
{
    private Server server;
    private ServerConnector connector;

    private void startServer(Function<SslConnectionFactory, OptionalSslConnectionFactory> configFn, Handler handler) throws Exception
    {
        QueuedThreadPool serverThreads = new QueuedThreadPool();
        serverThreads.setName("server");
        server = new Server(serverThreads);

        String keystore = MavenTestingUtils.getTestResourceFile("keystore").getAbsolutePath();
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(keystore);
        sslContextFactory.setKeyStorePassword("storepwd");
        sslContextFactory.setKeyManagerPassword("keypwd");

        HttpConfiguration httpConfig = new HttpConfiguration();
        HttpConnectionFactory http = new HttpConnectionFactory(httpConfig);
        SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, http.getProtocol());
        OptionalSslConnectionFactory sslOrOther = configFn.apply(ssl);
        connector = new ServerConnector(server, 1, 1, sslOrOther, ssl, http);
        server.addConnector(connector);

        server.setHandler(handler);

        server.start();
    }

    @AfterEach
    public void stopServer() throws Exception
    {
        if (server != null)
            server.stop();
    }

    private OptionalSslConnectionFactory optionalSsl(SslConnectionFactory ssl)
    {
        return new OptionalSslConnectionFactory(ssl, ssl.getNextProtocol());
    }

    private OptionalSslConnectionFactory optionalSslNoOtherProtocol(SslConnectionFactory ssl)
    {
        return new OptionalSslConnectionFactory(ssl, null);
    }

    @Test
    public void testOptionalSslConnection() throws Exception
    {
        startServer(this::optionalSsl, new EmptyServerHandler());

        String request = "" +
                "GET / HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        byte[] requestBytes = request.getBytes(StandardCharsets.US_ASCII);

        // Try first a plain text connection.
        try (Socket plain = new Socket())
        {
            plain.connect(new InetSocketAddress("localhost", connector.getLocalPort()), 1000);
            OutputStream plainOutput = plain.getOutputStream();
            plainOutput.write(requestBytes);
            plainOutput.flush();

            plain.setSoTimeout(5000);
            InputStream plainInput = plain.getInputStream();
            HttpTester.Response response = HttpTester.parseResponse(plainInput);
            assertNotNull(response);
            assertEquals(HttpStatus.OK_200, response.getStatus());
        }

        // Then try a SSL connection.
        SslContextFactory sslContextFactory = new SslContextFactory(true);
        sslContextFactory.start();
        try (Socket ssl = sslContextFactory.newSslSocket())
        {
            ssl.connect(new InetSocketAddress("localhost", connector.getLocalPort()), 1000);
            OutputStream sslOutput = ssl.getOutputStream();
            sslOutput.write(requestBytes);
            sslOutput.flush();

            ssl.setSoTimeout(5000);
            InputStream sslInput = ssl.getInputStream();
            HttpTester.Response response = HttpTester.parseResponse(sslInput);
            assertNotNull(response);
            assertEquals(HttpStatus.OK_200, response.getStatus());
        }
        finally
        {
            sslContextFactory.stop();
        }
    }

    @Test
    public void testOptionalSslConnectionWithOnlyOneByteShouldIdleTimeout() throws Exception
    {
        startServer(this::optionalSsl, new EmptyServerHandler());
        long idleTimeout = 1000;
        connector.setIdleTimeout(idleTimeout);

        try (Socket socket = new Socket())
        {
            socket.connect(new InetSocketAddress("localhost", connector.getLocalPort()), 1000);
            OutputStream output = socket.getOutputStream();
            output.write(0x16);
            output.flush();

            socket.setSoTimeout((int)(2 * idleTimeout));
            InputStream input = socket.getInputStream();
            int read = input.read();
            assertEquals(-1, read);
        }
    }

    @Test
    public void testOptionalSslConnectionWithUnknownBytes() throws Exception
    {
        startServer(this::optionalSslNoOtherProtocol, new EmptyServerHandler());

        try (Socket socket = new Socket())
        {
            socket.connect(new InetSocketAddress("localhost", connector.getLocalPort()), 1000);
            OutputStream output = socket.getOutputStream();
            output.write(0x00);
            output.flush();
            Thread.sleep(500);
            output.write(0x00);
            output.flush();

            socket.setSoTimeout(5000);
            InputStream input = socket.getInputStream();
            int read = input.read();
            assertEquals(-1, read);
        }
    }

    @Test
    public void testOptionalSslConnectionWithHTTPBytes() throws Exception
    {
        startServer(this::optionalSslNoOtherProtocol, new EmptyServerHandler());

        String request = "" +
                "GET / HTTP/1.1\r\n" +
                "Host: localhost\r\n" +
                "\r\n";
        byte[] requestBytes = request.getBytes(StandardCharsets.US_ASCII);

        // Send a plain text HTTP request to SSL port,
        // we should get back a minimal HTTP response.
        try (Socket socket = new Socket())
        {
            socket.connect(new InetSocketAddress("localhost", connector.getLocalPort()), 1000);
            OutputStream output = socket.getOutputStream();
            output.write(requestBytes);
            output.flush();

            socket.setSoTimeout(5000);
            InputStream input = socket.getInputStream();
            HttpTester.Response response = HttpTester.parseResponse(input);
            assertNotNull(response);
            assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatus());
        }
    }

    private static class EmptyServerHandler extends AbstractHandler.ErrorDispatchHandler
    {
        @Override
        protected void doNonErrorHandle(String target, Request jettyRequest, HttpServletRequest request, HttpServletResponse response)
        {
            jettyRequest.setHandled(true);
        }
    }
}
