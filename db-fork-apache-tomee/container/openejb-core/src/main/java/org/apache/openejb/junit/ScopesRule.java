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
package org.apache.openejb.junit;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.web.lifecycle.test.MockHttpSession;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import javax.enterprise.context.SessionScoped;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class ScopesRule implements TestRule {
    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final CdiScopes annotation = description.getAnnotation(CdiScopes.class);
                if (annotation == null) {
                    base.evaluate();
                    return;
                }

                final WebBeansContext wbc = WebBeansContext.currentInstance();
                final Map<Class<?>, Object> init = new HashMap<>();
                Class<? extends Annotation>[] scopes = annotation.value();
                for (final Class<? extends Annotation> c : scopes) {
                    final Object o = param(c);
                    if (o != null) {
                        init.put(c, o);
                    }
                    wbc.getContextsService().startContext(c, o);
                }
                try {
                    base.evaluate();
                } finally {
                    for (final Class<? extends Annotation> c : scopes) {
                        wbc.getContextsService().endContext(c, init.get(c));
                    }
                }
            }
        };
    }

    private Object param(final Class<?> c) {
        if (SessionScoped.class == c) {
            return new MockHttpSession();
        }
        return null;
    }
}
