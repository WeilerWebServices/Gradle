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

import org.eclipse.jetty.client.api.Response;

public class HttpResponseException extends RuntimeException
{
    private final Response response;

    public HttpResponseException(String message, Response response)
    {
        this(message, response, null);
    }
    
    public HttpResponseException(String message, Response response, Throwable cause)
    {
        super(message, cause);
        this.response = response;
    }

    public Response getResponse()
    {
        return response;
    }
}
