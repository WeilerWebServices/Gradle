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
package groovy.bugs

import gls.CompilableTestSupport

class Groovy8046Bug extends CompilableTestSupport {
    void testFieldShouldNotHavePrimitiveVoidType() {
        def message = shouldNotCompile """
            class MyClass {
                void field
            }
        """
        assert message.contains("The field 'field' has invalid type void") ||
                message.contains("void is not allowed here")
    }

    void testParameterShouldNotHavePrimitiveVoidType() {
        def message = shouldNotCompile """
            class MyClass {
                int foo(void param) {}
            }
        """
        assert message.contains("The parameter 'param' in method 'int foo(void)' has invalid type void") ||
                message.contains("void is not allowed here")
    }

    void testLocalVariableShouldNotHavePrimitiveVoidType() {
        def message = shouldNotCompile """
            class MyClass {
                def foo() {
                    void bar = null
                }
            }
        """
        assert message.contains("The variable 'bar' has invalid type void") ||
                message.contains("void is not allowed here")
    }
}
