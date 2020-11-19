/*
 *     Licensed to the Apache Software Foundation (ASF) under one or more
 *     contributor license agreements.  See the NOTICE file distributed with
 *     this work for additional information regarding copyright ownership.
 *     The ASF licenses this file to You under the Apache License, Version 2.0
 *     (the "License"); you may not use this file except in compliance with
 *     the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.apache.openejb.server.cxf.rs.event;

import org.apache.cxf.endpoint.Server;
import org.apache.openejb.AppContext;
import org.apache.openejb.core.WebContext;
import org.apache.openejb.observer.Event;

@Event
public class ServerCreated {
    private final Server server;
    private final AppContext appContext;
    private final WebContext webContext;
    private final String mapping;

    public ServerCreated(final Server server, final AppContext appContext, final WebContext webContext, final String mapping) {
        this.server = server;
        this.appContext = appContext;
        this.webContext = webContext;
        this.mapping = mapping;
    }

    public String getMapping() {
        return mapping;
    }

    public Server getServer() {
        return server;
    }

    public AppContext getAppContext() {
        return appContext;
    }

    public WebContext getWebContext() {
        return webContext;
    }

    @Override
    public String toString() {
        return "ServerCreated{" +
                "appContext=" + appContext.getId() +
                ", webContext=" + webContext.getHost() + '/' + webContext.getId() +
                '}';
    }
}
