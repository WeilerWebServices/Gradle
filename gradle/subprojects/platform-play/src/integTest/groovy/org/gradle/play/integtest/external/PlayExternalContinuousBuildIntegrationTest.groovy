/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.play.integtest.external

import org.gradle.integtests.fixtures.ToBeFixedForConfigurationCache
import org.gradle.play.integtest.fixtures.external.AbstractMultiVersionPlayExternalContinuousBuildIntegrationTest
import org.gradle.play.integtest.fixtures.external.BasicPlayApp
import org.gradle.play.integtest.fixtures.external.PlayApp
import org.gradle.play.integtest.fixtures.external.RunningPlayApp

class PlayExternalContinuousBuildIntegrationTest extends AbstractMultiVersionPlayExternalContinuousBuildIntegrationTest {
    RunningPlayApp runningApp = new RunningPlayApp(testDirectory)
    PlayApp playApp = new BasicPlayApp(versionNumber)

    def "build does not block when running play app with continuous build" () {
        when: "the build runs until it enters continuous build"
        succeeds("runPlay")

        then:
        appIsRunningAndDeployed()
    }

    @ToBeFixedForConfigurationCache
    def "can run play app multiple times with continuous build" () {
        when:
        succeeds("runPlay")

        then:
        appIsRunningAndDeployed()

        when:
        file("conf/routes") << "\n# changed"

        then:
        succeeds()

        when:
        file("conf/routes") << "\n# changed again"

        then:
        succeeds()

        when:
        file("conf/routes") << "\n# changed yet again"

        then:
        succeeds()
    }

    @ToBeFixedForConfigurationCache
    def "build failure prior to launch does not prevent launch on subsequent build" () {
        executer.withStackTraceChecksDisabled()
        def original = file("app/controllers/Application.scala").text

        when: "source file is broken"
        file("app/controllers/Application.scala").text = "class Application extends Controller {"

        then:
        fails("runPlay")

        when: "source file is fixed"
        file("app/controllers/Application.scala").text = original

        then:
        succeeds()

        and:
        appIsRunningAndDeployed()
    }

    def "play application is stopped when build is cancelled" () {
        when:
        succeeds("runPlay")

        then:
        appIsRunningAndDeployed()

        when:
        gradle.cancel()

        then:
        cancelsAndExits()
    }
}
