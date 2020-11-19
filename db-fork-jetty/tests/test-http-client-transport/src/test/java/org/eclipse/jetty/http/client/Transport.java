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

public enum Transport
{
    HTTP, HTTPS, H2C, H2, FCGI, UNIX_SOCKET;

    public boolean isHttp1Based()
    {
        return this == HTTP || this == HTTPS;
    }

    public boolean isHttp2Based()
    {
        return this == H2C || this == H2;
    }

    public boolean isTlsBased()
    {
        return this == HTTPS || this == H2;
    }

}
