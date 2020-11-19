/*
 * Copyright 2016 the original author or authors.
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


package org.gradle.testkit.runner.enduser

import org.gradle.integtests.fixtures.Sample
import org.gradle.integtests.fixtures.ToBeFixedForConfigurationCache
import org.gradle.integtests.fixtures.UsesSample
import org.gradle.integtests.fixtures.executer.GradleContextualExecuter
import org.gradle.testing.internal.util.RetryUtil
import org.gradle.testkit.runner.fixtures.NoDebug
import org.gradle.testkit.runner.fixtures.NonCrossVersion
import org.gradle.util.Requires
import org.junit.Rule
import spock.lang.IgnoreIf
import spock.lang.Unroll

import static org.gradle.util.TestPrecondition.JDK8_OR_EARLIER
import static org.gradle.util.TestPrecondition.ONLINE

@NonCrossVersion
@NoDebug
@IgnoreIf({ GradleContextualExecuter.embedded }) // These tests run builds that themselves run a build in a test worker with 'gradleTestKit()' dependency, which needs to pick up Gradle modules from a real distribution
class GradleRunnerSamplesEndUserIntegrationTest extends BaseTestKitEndUserIntegrationTest {

    @Rule
    Sample sample = new Sample(testDirectoryProvider)

    def setup() {
        executer.withRepositoryMirrors()
    }

    @Unroll
    @UsesSample("testKit/junitQuickstart")
    @ToBeFixedForConfigurationCache(iterationMatchers = ".*kotlin dsl.*")
    def "junitQuickstart with #dsl dsl"() {
        expect:
        executer.inDirectory(sample.dir.file(dsl))
        succeeds "check"

        where:
        dsl << ['groovy', 'kotlin']
    }

    @UsesSample("testKit/spockQuickstart")
    @ToBeFixedForConfigurationCache(because = "gradle/configuration-cache#270")
    def spockQuickstart() {
        expect:
        executer.inDirectory(sample.dir.file('groovy'))
        succeeds "check"
    }

    @Unroll
    @UsesSample("testKit/manualClasspathInjection")
    @Requires(JDK8_OR_EARLIER)
    @ToBeFixedForConfigurationCache(iterationMatchers = ".*kotlin dsl.*")
    // Uses Gradle 2.8 which does not support Java 9
    def "manualClasspathInjection with #dsl dsl"() {
        expect:
        executer.inDirectory(sample.dir.file(dsl))
        succeeds "check", '--stacktrace', '--info'

        where:
        dsl << ['groovy', 'kotlin']
    }

    @Unroll
    @UsesSample("testKit/automaticClasspathInjectionQuickstart")
    def "automaticClasspathInjectionQuickstart with #dsl dsl"() {
        expect:
        executer.inDirectory(sample.dir.file(dsl))
        succeeds "check"

        where:
        dsl << ['groovy', 'kotlin']
    }

    @Unroll
    @UsesSample("testKit/automaticClasspathInjectionCustomTestSourceSet")
    def "automaticClasspathInjectionCustomTestSourceSet with #dsl dsl"() {
        expect:
        executer.inDirectory(sample.dir.file(dsl))
        succeeds "check"

        where:
        dsl << ['groovy', 'kotlin']
    }

    @Requires([ONLINE, JDK8_OR_EARLIER])
    // Uses Gradle 2.6 which does not support Java 9
    @UsesSample("testKit/gradleVersion")
    @ToBeFixedForConfigurationCache(because = "gradle/configuration-cache#270")
    def gradleVersion() {
        expect:
        RetryUtil.retry { //This test is also affected by gradle/gradle#1111 on Windows
            executer.inDirectory(sample.dir.file('groovy'))
            succeeds "check"

        }
    }
}
