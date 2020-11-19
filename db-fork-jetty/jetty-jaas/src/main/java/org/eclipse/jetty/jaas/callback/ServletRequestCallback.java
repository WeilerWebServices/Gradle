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


package org.eclipse.jetty.jaas.callback;

import javax.security.auth.callback.Callback;
import javax.servlet.ServletRequest;


/**
 * ServletRequestCallback
 *
 * Provides access to the request associated with the authentication.
 */
public class ServletRequestCallback implements Callback
{
    protected ServletRequest _request;
    
    public void setRequest (ServletRequest request)
    {
        _request = request;
    }
 
    public ServletRequest getRequest ()
    {
        return _request;
    }
}
