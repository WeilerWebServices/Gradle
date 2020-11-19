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
package org.apache.openejb.resource.jdbc.dbcp;

import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.spi.ContainerSystem;

import java.io.ObjectStreamException;
import java.io.Serializable;
import javax.naming.NamingException;

public class DataSourceSerialization implements Serializable {
    private final String name;

    public DataSourceSerialization(String name) {
        this.name = name;
    }

    Object readResolve() throws ObjectStreamException {
        try {
            return SystemInstance.get().getComponent(ContainerSystem.class).getJNDIContext().lookup("openejb:Resource/" + name);
        } catch (final NamingException e) {
            throw new IllegalStateException(e);
        }
    }
}
