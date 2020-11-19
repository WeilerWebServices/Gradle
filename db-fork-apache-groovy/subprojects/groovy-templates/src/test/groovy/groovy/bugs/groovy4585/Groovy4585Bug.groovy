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
package groovy.bugs.groovy4585

class Groovy4585Bug extends GroovyTestCase {
    void test() {
        def engineForBuildXml = new groovy.text.SimpleTemplateEngine(false)
        engineForBuildXml.setEscapeBackslash(true)
        def templateForBuildXml = engineForBuildXml.createTemplate(this.getClass().getResource("/bugs/groovy4585/groovy4585.xml").text)
        String buildXmlContent = templateForBuildXml.make([names:['a', 'b', 'c']]).toString()

        assert buildXmlContent.contains('<property name="drive" value="d:\\" />')
        assert buildXmlContent.contains('<exec dir="${drive}" executable="echo">')
    }
}
