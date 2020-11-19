/*
 * Copyright 2014 the original author or authors.
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



package org.gradle.play.plugins
import org.gradle.integtests.fixtures.WellBehavedPluginTest

class PlayJavaScriptPluginGoodBehaviourIntegrationTest extends WellBehavedPluginTest {

    def setup() {
        executer.expectDeprecationWarning()
    }

    @Override
    String getPluginName() {
        return 'play-javascript'
    }

    def "emits deprecation warning"() {
        given:
        applyPlugin()

        when:
        succeeds("help")

        then:
        outputContains("The Play JavaScript plugin has been deprecated. This is scheduled to be removed in Gradle 7.0. Consider using the org.gradle.playframework-javascript plugin instead.")
    }
}
