/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.codehaus.groovy.vmplugin;

import org.codehaus.groovy.vmplugin.v7.Java7;

/**
 * Factory class to get functionality based on the VM version.
 * The usage of this class is not for public use, only for the
 * runtime.
 */
public class VMPluginFactory {

    private static final String JDK8_CLASSNAME_CHECK = "java.util.Optional";
    private static final String JDK9_CLASSNAME_CHECK = "java.lang.Module";

    private static final String JDK8_PLUGIN_NAME = "org.codehaus.groovy.vmplugin.v8.Java8";
    private static final String JDK9_PLUGIN_NAME = "org.codehaus.groovy.vmplugin.v9.Java9";

    private static final VMPlugin plugin;

    static {
        VMPlugin target = createPlugin(JDK9_CLASSNAME_CHECK, JDK9_PLUGIN_NAME);
        if (target == null) {
            target = createPlugin(JDK8_CLASSNAME_CHECK, JDK8_PLUGIN_NAME);
            if (target == null) {
                target = new Java7();
            }
        }
        plugin = target;
    }

    public static VMPlugin getPlugin() {
        return plugin;
    }

    private static VMPlugin createPlugin(String classNameCheck, String pluginName) {
        try {
            ClassLoader loader = VMPluginFactory.class.getClassLoader();
            loader.loadClass(classNameCheck);
            return (VMPlugin) loader.loadClass(pluginName).newInstance();
        } catch (Throwable ex) {
            return null;
        }
    }
}
