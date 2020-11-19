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

package org.apache.openejb.core.singleton;

import org.apache.openejb.BeanContext;
import org.apache.openejb.InterfaceType;
import org.apache.openejb.core.ivm.EjbHomeProxyHandler;
import org.apache.openejb.core.ivm.EjbObjectProxyHandler;

import javax.ejb.RemoveException;
import java.lang.reflect.Method;
import java.util.List;

public class SingletonEjbHomeHandler extends EjbHomeProxyHandler {

    public SingletonEjbHomeHandler(final BeanContext beanContext, final InterfaceType interfaceType, final List<Class> interfaces, final Class mainInterface) {
        super(beanContext, interfaceType, interfaces, mainInterface);
    }

    protected Object findX(final Class interfce, final Method method, final Object[] args, final Object proxy) throws Throwable {
        throw new UnsupportedOperationException("Stateful beans may not have find methods");
    }

    protected Object removeByPrimaryKey(final Class interfce, final Method method, final Object[] args, final Object proxy) throws Throwable {
        throw new RemoveException("Session objects are private resources and do not have primary keys");
    }

    protected Object removeWithHandle(final Class interfce, final Method method, final Object[] args, final Object proxy) throws Throwable {
        // stateless can't be removed
        return null;
    }

    protected EjbObjectProxyHandler newEjbObjectHandler(final BeanContext beanContext, final Object pk, final InterfaceType interfaceType, final List<Class> interfaces, final Class mainInterface) {
        return new SingletonEjbObjectHandler(getBeanContext(), pk, interfaceType, interfaces, mainInterface);
    }

}
