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
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError

testPostfixExpression()

/***********************************/

void testPostfixExpression() {
    isRendered """
assert x++ == null
       ||  |
       |0  false
       0
        """, {
        def x = 0
        assert x++ == null
    }
}

static isRendered(String expectedRendering, Closure failingAssertion) {
    try {
        failingAssertion.call();
        assert false, "assertion should have failed but didn't"
    } catch (PowerAssertionError e) {
        assert expectedRendering.trim() == e.message.trim()
    }
}
