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
package org.apache.groovy.ast.tools;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;

import static org.apache.groovy.util.BeanUtils.decapitalize;

/**
 * Utility class for working with MethodNodes
 */
public class MethodNodeUtils {
    /**
     * Return the method node's descriptor including its
     * name and parameter types without generics.
     *
     * @param mNode the method node
     * @return the method node's abbreviated descriptor excluding the return type
     */
    public static String methodDescriptorWithoutReturnType(MethodNode mNode) {
        StringBuilder sb = new StringBuilder();
        sb.append(mNode.getName()).append(':');
        for (Parameter p : mNode.getParameters()) {
            sb.append(ClassNodeUtils.formatTypeName(p.getType())).append(',');
        }
        return sb.toString();
    }

    /**
     * Return the method node's descriptor which includes its return type,
     * name and parameter types without generics.
     *
     * @param mNode the method node
     * @return the method node's descriptor
     */
    public static String methodDescriptor(MethodNode mNode) {
        StringBuilder sb = new StringBuilder(mNode.getName().length() + mNode.getParameters().length * 10);
        sb.append(mNode.getReturnType().getName());
        sb.append(' ');
        sb.append(mNode.getName());
        sb.append('(');
        for (int i = 0; i < mNode.getParameters().length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Parameter p = mNode.getParameters()[i];
            sb.append(ClassNodeUtils.formatTypeName(p.getType()));
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * For a method node potentially representing a property, returns the name of the property.
     *
     * @param mNode a MethodNode
     * @return the property name without the get/set/is prefix if a property or null
     */
    public static String getPropertyName(MethodNode mNode) {
        String name = mNode.getName();
        if (name.startsWith("set") || name.startsWith("get") || name.startsWith("is")) {
            String pname = decapitalize(name.substring(name.startsWith("is") ? 2 : 3));
            if (!pname.isEmpty()) {
                if (name.startsWith("set")) {
                    if (mNode.getParameters().length == 1) {
                        return pname;
                    }
                } else if (mNode.getParameters().length == 0 && !ClassHelper.VOID_TYPE.equals(mNode.getReturnType())) {
                    if (name.startsWith("get") || ClassHelper.boolean_TYPE.equals(mNode.getReturnType())) {
                        return pname;
                    }
                }
            }
        }
        return null;
    }

    private MethodNodeUtils() { }
}
