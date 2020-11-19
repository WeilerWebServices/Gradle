/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.openejb.core.ivm;

import org.apache.openejb.core.rmi.BlacklistClassResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;

/**
 * @version $Rev$ $Date$
 */
public class EjbObjectInputStream extends ObjectInputStream {

    public EjbObjectInputStream(final InputStream in) throws IOException {
        super(in);
    }

    @Override
    protected Class resolveClass(final ObjectStreamClass classDesc) throws IOException, ClassNotFoundException {
        final String checkedName = BlacklistClassResolver.DEFAULT.check(classDesc.getName());
        try {
            return Class.forName(checkedName, false, getClassloader());
        } catch (final ClassNotFoundException e) {
            final String n = classDesc.getName();
            if (n.equals("boolean")) {
                return boolean.class;
            }
            if (n.equals("byte")) {
                return byte.class;
            }
            if (n.equals("char")) {
                return char.class;
            }
            if (n.equals("short")) {
                return short.class;
            }
            if (n.equals("int")) {
                return int.class;
            }
            if (n.equals("long")) {
                return long.class;
            }
            if (n.equals("float")) {
                return float.class;
            }
            if (n.equals("double")) {
                return double.class;
            }

            return getClass().getClassLoader().loadClass(checkedName); // if CCL is not correct
        }
    }

    protected Class resolveProxyClass(final String[] interfaces) throws IOException, ClassNotFoundException {
        final Class[] cinterfaces = new Class[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            cinterfaces[i] = getClassloader().loadClass(interfaces[i]);
        }

        try {
            return Proxy.getProxyClass(getClassloader(), cinterfaces);
        } catch (final IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }
    }

    ClassLoader getClassloader() {
        return Thread.currentThread().getContextClassLoader();
    }

}
