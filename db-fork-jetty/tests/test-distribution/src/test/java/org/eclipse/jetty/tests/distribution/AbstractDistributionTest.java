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

package org.eclipse.jetty.tests.distribution;

import java.util.function.Supplier;

import org.eclipse.jetty.client.HttpClient;
import org.junit.jupiter.api.AfterEach;

public class AbstractDistributionTest
{
    protected HttpClient client;

    protected void startHttpClient() throws Exception
    {
        startHttpClient(HttpClient::new);
    }

    protected void startHttpClient(Supplier<HttpClient> supplier) throws Exception
    {
        client = supplier.get();
        client.start();
    }

    @AfterEach
    public void dispose() throws Exception
    {
        if (client != null)
            client.stop();
    }
}
