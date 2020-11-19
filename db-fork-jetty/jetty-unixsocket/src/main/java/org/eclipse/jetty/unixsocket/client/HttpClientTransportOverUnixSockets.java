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

package org.eclipse.jetty.unixsocket.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ManagedSelector;
import org.eclipse.jetty.io.SelectorManager;
import org.eclipse.jetty.io.ssl.SslClientConnectionFactory;
import org.eclipse.jetty.unixsocket.UnixSocketEndPoint;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import jnr.enxio.channels.NativeSelectorProvider;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

public class HttpClientTransportOverUnixSockets
    extends HttpClientTransportOverHTTP
{
    private static final Logger LOG = Log.getLogger( HttpClientTransportOverUnixSockets.class );
    
    private String _unixSocket;
    private SelectorManager selectorManager;

    private UnixSocketChannel channel;

    public HttpClientTransportOverUnixSockets( String unixSocket )
    {
        if ( unixSocket == null )
        {
            throw new IllegalArgumentException( "Unix socket file cannot be null" );
        }
        this._unixSocket = unixSocket;
    }

    @Override
    protected SelectorManager newSelectorManager(HttpClient client)
    {
        return selectorManager = new UnixSocketSelectorManager(client,getSelectors());
    }

    @Override
    public void connect( InetSocketAddress address, Map<String, Object> context )
    {

        try
        {
            InetAddress inet = address.getAddress();
            if (!inet.isLoopbackAddress() && !inet.isLinkLocalAddress() && !inet.isSiteLocalAddress())
                throw new IOException("UnixSocket cannot connect to "+address.getHostString());
            
            // Open a unix socket
            UnixSocketAddress unixAddress = new UnixSocketAddress( this._unixSocket );
            channel = UnixSocketChannel.open( unixAddress );
            
            HttpDestination destination = (HttpDestination)context.get(HTTP_DESTINATION_CONTEXT_KEY);
            HttpClient client = destination.getHttpClient();

            configure(client, channel);

            channel.configureBlocking(false);
            selectorManager.accept(channel, context);            
        }
        // Must catch all exceptions, since some like
        // UnresolvedAddressException are not IOExceptions.
        catch (Throwable x)
        {
            // If IPv6 is not deployed, a generic SocketException "Network is unreachable"
            // exception is being thrown, so we attempt to provide a better error message.
            if (x.getClass() == SocketException.class)
                x = new SocketException("Could not connect to " + address).initCause(x);

            try
            {
                if (channel != null)
                    channel.close();
            }
            catch (IOException xx)
            {
                LOG.ignore(xx);
            }
            finally
            {
                connectFailed(context, x);
            }
        }
    }

    public class UnixSocketSelectorManager extends ClientSelectorManager
    {
        protected UnixSocketSelectorManager(HttpClient client, int selectors)
        {
            super(client,selectors);
        }

        @Override
        protected Selector newSelector() throws IOException
        {
            return NativeSelectorProvider.getInstance().openSelector();
        }

        @Override
        protected EndPoint newEndPoint(SelectableChannel channel, ManagedSelector selector, SelectionKey key)
        {
            UnixSocketEndPoint endp = new UnixSocketEndPoint((UnixSocketChannel)channel, selector, key, getScheduler());
            endp.setIdleTimeout(getHttpClient().getIdleTimeout());
            return endp;
        }
    }

    @Override
    protected void doStop()
        throws Exception
    {
        super.doStop();
        try
        {
            if (channel != null)
                channel.close();
        }
        catch (IOException xx)
        {
            LOG.ignore(xx);
        }
    }
}
