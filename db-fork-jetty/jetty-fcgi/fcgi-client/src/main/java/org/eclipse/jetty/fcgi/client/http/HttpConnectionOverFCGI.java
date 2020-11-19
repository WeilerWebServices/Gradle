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

package org.eclipse.jetty.fcgi.client.http;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jetty.client.HttpChannel;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpConnection;
import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.client.SendFailure;
import org.eclipse.jetty.client.api.Connection;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.fcgi.FCGI;
import org.eclipse.jetty.fcgi.generator.Flusher;
import org.eclipse.jetty.fcgi.parser.ClientParser;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpHeaderValue;
import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.CompletableCallback;
import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

public class HttpConnectionOverFCGI extends AbstractConnection implements Connection
{
    private static final Logger LOG = Log.getLogger(HttpConnectionOverFCGI.class);

    private final LinkedList<Integer> requests = new LinkedList<>();
    private final Map<Integer, HttpChannelOverFCGI> activeChannels = new ConcurrentHashMap<>();
    private final Queue<HttpChannelOverFCGI> idleChannels = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final HttpDestination destination;
    private final Promise<Connection> promise;
    private final boolean multiplexed;
    private final Flusher flusher;
    private final Delegate delegate;
    private final ClientParser parser;
    private ByteBuffer buffer;

    public HttpConnectionOverFCGI(EndPoint endPoint, HttpDestination destination, Promise<Connection> promise, boolean multiplexed)
    {
        super(endPoint, destination.getHttpClient().getExecutor());
        this.destination = destination;
        this.promise = promise;
        this.multiplexed = multiplexed;
        this.flusher = new Flusher(endPoint);
        this.delegate = new Delegate(destination);
        this.parser = new ClientParser(new ResponseListener());
        requests.addLast(0);
    }

    public HttpDestination getHttpDestination()
    {
        return destination;
    }

    protected Flusher getFlusher()
    {
        return flusher;
    }

    @Override
    public void send(Request request, Response.CompleteListener listener)
    {
        delegate.send(request, listener);
    }

    protected SendFailure send(HttpExchange exchange)
    {
        return delegate.send(exchange);
    }

    @Override
    public void onOpen()
    {
        super.onOpen();
        fillInterested();
        promise.succeeded(this);
    }

    @Override
    public void onFillable()
    {
        buffer = acquireBuffer();
        process(buffer);
    }

    private ByteBuffer acquireBuffer()
    {
        HttpClient client = destination.getHttpClient();
        ByteBufferPool bufferPool = client.getByteBufferPool();
        return bufferPool.acquire(client.getResponseBufferSize(), true);
    }

    private void releaseBuffer(ByteBuffer buffer)
    {
        @SuppressWarnings("ReferenceEquality")
        boolean isCurrentBuffer = (this.buffer == buffer);
        assert(isCurrentBuffer);
        HttpClient client = destination.getHttpClient();
        ByteBufferPool bufferPool = client.getByteBufferPool();
        bufferPool.release(buffer);
        this.buffer = null;
    }

    private void process(ByteBuffer buffer)
    {
        try
        {
            EndPoint endPoint = getEndPoint();
            boolean looping = false;
            while (true)
            {
                if (!looping && parse(buffer))
                    return;

                int read = endPoint.fill(buffer);
                if (LOG.isDebugEnabled())
                    LOG.debug("Read {} bytes from {}", read, endPoint);

                if (read > 0)
                {
                    if (parse(buffer))
                        return;
                }
                else if (read == 0)
                {
                    releaseBuffer(buffer);
                    fillInterested();
                    return;
                }
                else
                {
                    releaseBuffer(buffer);
                    shutdown();
                    return;
                }

                looping = true;
            }
        }
        catch (Exception x)
        {
            if (LOG.isDebugEnabled())
                LOG.debug(x);
            releaseBuffer(buffer);
            close(x);
        }
    }

    private boolean parse(ByteBuffer buffer)
    {
        return parser.parse(buffer);
    }

    private void shutdown()
    {
        // Close explicitly only if we are idle, since the request may still
        // be in progress, otherwise close only if we can fail the responses.
        if (activeChannels.isEmpty())
            close();
        else
            failAndClose(new EOFException(String.valueOf(getEndPoint())));
    }

    @Override
    public boolean onIdleExpired()
    {
        long idleTimeout = getEndPoint().getIdleTimeout();
        boolean close = delegate.onIdleTimeout(idleTimeout);
        if (multiplexed)
            close &= isFillInterested();
        if (close)
            close(new TimeoutException("Idle timeout " + idleTimeout + " ms"));
        return false;
    }

    protected void release(HttpChannelOverFCGI channel)
    {
        if (activeChannels.remove(channel.getRequest()) == null)
        {
            channel.destroy();
        }
        else
        {
            channel.setRequest(0);
            // Recycle only non-failed channels.
            if (channel.isFailed())
                channel.destroy();
            else
                idleChannels.offer(channel);
            destination.release(this);
        }
    }

    @Override
    public void close()
    {
        close(new AsynchronousCloseException());
    }

    protected void close(Throwable failure)
    {
        if (closed.compareAndSet(false, true))
        {
            getHttpDestination().close(this);

            abort(failure);

            getEndPoint().shutdownOutput();
            if (LOG.isDebugEnabled())
                LOG.debug("Shutdown {}", this);
            getEndPoint().close();
            if (LOG.isDebugEnabled())
                LOG.debug("Closed {}", this);
        }
    }

    @Override
    public boolean isClosed()
    {
        return closed.get();
    }

    protected boolean closeByHTTP(HttpFields fields)
    {
        if (multiplexed)
            return false;
        if (!fields.contains(HttpHeader.CONNECTION, HttpHeaderValue.CLOSE.asString()))
            return false;
        close();
        return true;
    }

    protected void abort(Throwable failure)
    {
        for (HttpChannelOverFCGI channel : activeChannels.values())
        {
            HttpExchange exchange = channel.getHttpExchange();
            if (exchange != null)
                exchange.getRequest().abort(failure);
            channel.destroy();
        }
        activeChannels.clear();
        
        HttpChannel channel = idleChannels.poll();
        while (channel!=null)
        {
            channel.destroy();
            channel = idleChannels.poll();
        }
    }

    private void failAndClose(Throwable failure)
    {
        boolean result = false;
        for (HttpChannelOverFCGI channel : activeChannels.values())
        {
            result |= channel.responseFailure(failure);
            channel.destroy();
        }
        
        if (result)
            close(failure);
    }

    private int acquireRequest()
    {
        synchronized (requests)
        {
            int last = requests.getLast();
            int request = last + 1;
            requests.addLast(request);
            return request;
        }
    }

    private void releaseRequest(int request)
    {
        synchronized (requests)
        {
            requests.removeFirstOccurrence(request);
        }
    }

    protected HttpChannelOverFCGI acquireHttpChannel(int id, Request request)
    {
        HttpChannelOverFCGI channel = idleChannels.poll();
        if (channel == null)
            channel = newHttpChannel(request);
        channel.setRequest(id);
        return channel;
    }

    protected HttpChannelOverFCGI newHttpChannel(Request request)
    {
        return new HttpChannelOverFCGI(this, getFlusher(), request.getIdleTimeout());
    }

    @Override
    public String toConnectionString()
    {
        return String.format("%s@%x[l:%s<->r:%s]",
            getClass().getSimpleName(),
            hashCode(),
            getEndPoint().getLocalAddress(),
            getEndPoint().getRemoteAddress());
    }
    
    private class Delegate extends HttpConnection
    {
        private Delegate(HttpDestination destination)
        {
            super(destination);
        }

        @Override
        protected SendFailure send(HttpExchange exchange)
        {
            Request request = exchange.getRequest();
            normalizeRequest(request);

            // FCGI may be multiplexed, so one channel for each exchange.
            int id = acquireRequest();
            HttpChannelOverFCGI channel = acquireHttpChannel(id, request);
            activeChannels.put(id, channel);

            return send(channel, exchange);
        }

        @Override
        public void close()
        {
            HttpConnectionOverFCGI.this.close();
        }

        protected void close(Throwable failure)
        {
            HttpConnectionOverFCGI.this.close(failure);
        }

        @Override
        public boolean isClosed()
        {
            return HttpConnectionOverFCGI.this.isClosed();
        }

        @Override
        public String toString()
        {
            return HttpConnectionOverFCGI.this.toString();
        }
    }

    private class ResponseListener implements ClientParser.Listener
    {
        @Override
        public void onBegin(int request, int code, String reason)
        {
            HttpChannelOverFCGI channel = activeChannels.get(request);
            if (channel != null)
                channel.responseBegin(code, reason);
            else
                noChannel(request);
        }

        @Override
        public void onHeader(int request, HttpField field)
        {
            HttpChannelOverFCGI channel = activeChannels.get(request);
            if (channel != null)
                channel.responseHeader(field);
            else
                noChannel(request);
        }

        @Override
        public void onHeaders(int request)
        {
            HttpChannelOverFCGI channel = activeChannels.get(request);
            if (channel != null)
                channel.responseHeaders();
            else
                noChannel(request);
        }

        @Override
        public boolean onContent(int request, FCGI.StreamType stream, ByteBuffer buffer)
        {
            switch (stream)
            {
                case STD_OUT:
                {
                    HttpChannelOverFCGI channel = activeChannels.get(request);
                    if (channel != null)
                    {
                        CompletableCallback callback = new CompletableCallback()
                        {
                            @Override
                            public void resume()
                            {
                                if (LOG.isDebugEnabled())
                                    LOG.debug("Content consumed asynchronously, resuming processing");
                                process(HttpConnectionOverFCGI.this.buffer);
                            }

                            @Override
                            public void abort(Throwable x)
                            {
                                close(x);
                            }
                        };
                        // Do not short circuit these calls.
                        boolean proceed = channel.content(buffer, callback);
                        boolean async = callback.tryComplete();
                        return !proceed || async;
                    }
                    else
                    {
                        noChannel(request);
                    }
                    break;
                }
                case STD_ERR:
                {
                    LOG.info(BufferUtil.toUTF8String(buffer));
                    break;
                }
                default:
                {
                    throw new IllegalArgumentException();
                }
            }
            return false;
        }

        @Override
        public void onEnd(int request)
        {
            HttpChannelOverFCGI channel = activeChannels.get(request);
            if (channel != null)
            {
                if (channel.responseSuccess())
                    releaseRequest(request);
            }
            else
            {
                noChannel(request);
            }
        }

        @Override
        public void onFailure(int request, Throwable failure)
        {
            HttpChannelOverFCGI channel = activeChannels.get(request);
            if (channel != null)
            {
                if (channel.responseFailure(failure))
                    releaseRequest(request);
            }
            else
            {
                noChannel(request);
            }
        }

        private void noChannel(int request)
        {
            if (LOG.isDebugEnabled())
                LOG.debug("Channel not found for request {}", request);
        }
    }
}
