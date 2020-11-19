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
package groovy.transform;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class, method or constructor annotation which indicates that each parameter
 * should be checked to ensure it isn't null. If placed at the class level,
 * all explicit methods and constructors will be checked.
 * <p>
 * Example usage:
 * <pre class="groovyTestCase">
 * import groovy.transform.NullCheck
 * import static groovy.test.GroovyAssert.shouldFail
 *
 * {@code @NullCheck}
 * class Greeter {
 *     private String audience
 *
 *     Greeter(String audience) {
 *         this.audience = audience.toLowerCase()
 *     }
 *
 *     String greeting(String salutation) {
 *         salutation.toUpperCase() + ' ' + audience
 *     }
 * }
 *
 * assert new Greeter('World').greeting('hello') == 'HELLO world'
 *
 * def ex = shouldFail(IllegalArgumentException) { new Greeter(null) }
 * assert ex.message == 'audience cannot be null'
 *
 * ex = shouldFail(IllegalArgumentException) { new Greeter('Universe').greeting(null) }
 * assert ex.message == 'salutation cannot be null'
 * </pre>
 * The produced code for the above example looks like this:
 * <pre>
 * class Greeter {
 *     private String audience
 *
 *     Foo(String audience) {
 *         if (audience == null) throw new IllegalArgumentException('audience cannot be null')
 *         this.audience = audience.toLowerCase()
 *     }
 *
 *     String greeting(String salutation) {
 *         if (salutation == null) throw new IllegalArgumentException('salutation cannot be null')
 *         salutation.toUpperCase() + ' ' + audience
 *     }
 * }
 * </pre>
 *
 * @since 3.0.0
 */
@java.lang.annotation.Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
@GroovyASTTransformationClass("org.codehaus.groovy.transform.NullCheckASTTransformation")
public @interface NullCheck {
}
