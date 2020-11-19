/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.play.tasks

import org.gradle.integtests.fixtures.TargetCoverage
import org.gradle.play.integtest.fixtures.PlayCoverage

@TargetCoverage({ PlayCoverage.DEFAULT })
class Play24RoutesCompileIntegrationTest extends AbstractRoutesCompileIntegrationTest {

    @Override
    def getJavaRoutesFileName(String packageName, String namespace) {
        return "${namespace ? namespace + '/' :''}controllers/${packageName ? packageName + '/' :''}routes.java"
    }

    @Override
    def getReverseRoutesFileName(String packageName, String namespace) {
        return "${namespace ? namespace + '/' :''}controllers/${packageName ? packageName + '/' :''}ReverseRoutes.scala"
    }

    @Override
    def getScalaRoutesFileName(String packageName, String namespace) {
        return "${packageName?:'router'}/Routes.scala"
    }

    @Override
    def getOtherRoutesFileNames() {
        return [
            {packageName, namespace -> "${namespace ? namespace + '/' :''}controllers/${packageName ? packageName + '/' :''}javascript/JavaScriptReverseRoutes.scala" },
            {packageName, namespace -> "${packageName?:'router'}/RoutesPrefix.scala" }
        ]
    }

    def "can specify route compiler type as injected"() {
        given:
        withRoutesTemplate()
        withInjectedRoutesController()
        buildFile << """
model {
    components {
        play {
            injectedRoutesGenerator = true
        }
    }
}
"""
        expect:
        succeeds("compilePlayBinaryScala")
        and:
        destinationDir.assertHasDescendants(createRouteFileList() as String[])
    }

    def "recompiles when route compiler type is changed"() {
        when:
        withRoutesTemplate()
        then:
        succeeds("compilePlayBinaryScala")

        when:
        executer.noDeprecationChecks()
        withInjectedRoutesController()
        buildFile << """
model {
    components {
        play {
            injectedRoutesGenerator = true
        }
    }
}
"""
        then:
        succeeds("compilePlayBinaryScala")
        executed(":compilePlayBinaryPlayRoutes")
        and:
        destinationDir.assertHasDescendants(createRouteFileList() as String[])
    }

    private withInjectedRoutesController() {
        file("app/controllers/Application.scala").with {
            // change Scala companion object into a regular class
            text = text.replaceFirst(/object/, "class")
        }
    }


    def "failure to generate routes fails the build with useful message"() {
        given:
        file("conf/routes") << """
# This will cause route compilation failure since overload is not supported.
GET        /        com.foobar.HelloController.index()
GET        /*path   com.foobar.HelloController.index(path)
        """
        expect:
        fails("compilePlayBinaryPlayRoutes")
        result.assertHasErrorOutput("Using different overloaded methods is not allowed. If you are using a single method in combination with default parameters, make sure you declare them all explicitly.")
    }
}
