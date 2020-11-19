/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.openejb.server.cxf.rs;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.openejb.OpenEjbContainer;
import org.apache.openejb.config.DeploymentFilterable;
import org.apache.openejb.config.DeploymentLoader;
import org.apache.openejb.util.NetworkUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Properties;
import javax.ejb.Singleton;
import javax.ejb.embeddable.EJBContainer;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import static org.junit.Assert.assertEquals;

public class CustomProviderTest {
    private static EJBContainer container;
    private static int port = -1;

    @BeforeClass
    public static void start() throws Exception {
        port = NetworkUtil.getNextAvailablePort();
        final Properties properties = new Properties();
        properties.setProperty("httpejbd.port", Integer.toString(port));
        properties.setProperty(DeploymentFilterable.CLASSPATH_INCLUDE, ".*openejb-cxf-rs.*");
        properties.setProperty(OpenEjbContainer.OPENEJB_EMBEDDED_REMOTABLE, "true");
        properties.setProperty(DeploymentLoader.OPENEJB_ALTDD_PREFIX, "custom");

        container = EJBContainer.createEJBContainer(properties);
    }

    @AfterClass
    public static void close() throws Exception {
        if (container != null) {
            container.close();
        }
    }

    @Test
    public void customProvider() {
        final String response = WebClient.create("http://localhost:" + port + "/openejb-cxf-rs").accept("openejb/reverse")
            .path("/custom1/reverse").get(String.class);
        assertEquals("provider", response);
    }

    @Test
    public void customSpecificProvider() {
        final String response = WebClient.create("http://localhost:" + port + "/openejb-cxf-rs").accept("openejb/constant")
            .path("/custom2/constant").get(String.class);
        assertEquals("it works!", response);
    }

    @Singleton
    @Path("/custom1")
    public static class CustomService {
        @GET
        @Path("/reverse")
        @Produces("openejb/reverse")
        public Message go() {
            return new Message("redivorp");
        }
    }

    @Singleton
    @Path("/custom2")
    public static class CustomSpecificService {
        @GET
        @Path("/constant")
        @Produces("openejb/constant")
        public Message go() {
            return new Message("will be overriden");
        }
    }

    @Provider
    @Produces("openejb/reverse")
    public static class ReverseProvider<T> implements MessageBodyWriter<T> {
        private String reverse(String str) {
            if (str == null) {
                return "";
            }

            StringBuilder s = new StringBuilder(str.length());
            for (int i = str.length() - 1; i >= 0; i--) {
                s.append(str.charAt(i));
            }
            return s.toString();
        }

        @Override
        public long getSize(T t, Class<?> rawType, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public boolean isWriteable(Class<?> rawType, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return Message.class == rawType;
        }

        @Override
        public void writeTo(T t, Class<?> rawType, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
            entityStream.write(reverse(Message.class.cast(t).text).getBytes());
        }
    }

    @Provider
    @Produces("openejb/constant")
    public static class ConstantProvider<T> implements MessageBodyWriter<T> {
        @Override
        public long getSize(T t, Class<?> rawType, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return -1;
        }

        @Override
        public boolean isWriteable(Class<?> rawType, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return Message.class == rawType;
        }

        @Override
        public void writeTo(T t, Class<?> rawType, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
            entityStream.write("it works!".getBytes());
        }
    }

    public static class Message {
        private final String text;

        public Message(final String text) {
            this.text = text;
        }
    }
}
