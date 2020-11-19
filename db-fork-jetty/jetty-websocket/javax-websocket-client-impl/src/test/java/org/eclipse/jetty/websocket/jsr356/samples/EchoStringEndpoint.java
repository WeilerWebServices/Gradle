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

package org.eclipse.jetty.websocket.jsr356.samples;

import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.eclipse.jetty.util.BlockingArrayQueue;

/**
 * Legitimate structure for an Endpoint
 */
public class EchoStringEndpoint extends AbstractStringEndpoint
{
    public BlockingArrayQueue<String> messages = new BlockingArrayQueue<>();

    @Override
    public void onOpen(Session session, EndpointConfig config)
    {
        super.onOpen(session, config);
        this.session.getUserProperties().put("endpoint", this);
    }
    
    @Override
    public void onMessage(String message)
    {
        this.messages.offer(message);
        this.session.getAsyncRemote().sendText(message);
    }
}
