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

package org.apache.openejb.assembler.classic;

import org.apache.openejb.bval.ValidatorUtil;

import javax.naming.NamingException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LazyValidator implements InvocationHandler, Serializable {
    private transient ValidatorFactory factory;
    private transient volatile Validator validator;

    public LazyValidator(final ValidatorFactory factory) {
        this.factory = factory;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        ensureDelegate();
        try {
            return method.invoke(validator, args);
        } catch (final InvocationTargetException ite) {
            throw ite.getCause();
        }
    }

    private void ensureDelegate() {
        if (validator == null) {
            synchronized (this) {
                if (validator == null) {
                    if (validator == null) {
                        validator = (factory == null ? findFactory() : factory).usingContext().getValidator();
                    }
                }
            }
        }
    }

    private ValidatorFactory findFactory() {
        try {
            return ValidatorUtil.lookupFactory();
        } catch (final NamingException ne) {
            return Validation.buildDefaultValidatorFactory();
        }
    }

    public Validator getValidator() {
        ensureDelegate();
        return validator;
    }
}
