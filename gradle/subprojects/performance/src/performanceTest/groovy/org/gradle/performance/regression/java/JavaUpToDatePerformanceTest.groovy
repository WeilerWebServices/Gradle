/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.performance.regression.java

import org.gradle.initialization.StartParameterBuildOptions
import org.gradle.performance.AbstractCrossVersionPerformanceTest
import org.gradle.performance.annotations.RunFor
import org.gradle.performance.annotations.Scenario
import org.gradle.profiler.mutations.AbstractCleanupMutator
import org.gradle.profiler.mutations.ClearBuildCacheMutator
import spock.lang.Unroll

import static org.gradle.performance.annotations.ScenarioType.TEST
import static org.gradle.performance.results.OperatingSystem.LINUX
import static org.gradle.performance.results.OperatingSystem.WINDOWS

class JavaUpToDatePerformanceTest extends AbstractCrossVersionPerformanceTest {
    @RunFor([
        @Scenario(type = TEST, operatingSystems = [LINUX, WINDOWS], testProjects = ["largeJavaMultiProject"], iterationMatcher = '.*parallel true.*'),
        @Scenario(type = TEST, operatingSystems = [LINUX], testProjects = ["largeJavaMultiProject", "largeMonolithicJavaProject"], iterationMatcher = '.*parallel false.*'),
    ])
    @Unroll
    def "up-to-date assemble (parallel #parallel)"() {
        given:
        runner.tasksToRun = ['assemble']
        runner.targetVersions = ["6.8-20201116230039+0000"]
        runner.args += ["-Dorg.gradle.parallel=$parallel"]

        when:
        def result = runner.run()

        then:
        result.assertCurrentVersionHasNotRegressed()

        where:
        parallel << [true, false]
    }

    @RunFor([
        @Scenario(type = TEST, operatingSystems = [LINUX], testProjects = ["largeJavaMultiProject"], iterationMatcher = '.*parallel true.*'),
        @Scenario(type = TEST, operatingSystems = [LINUX], testProjects = ["largeJavaMultiProject", "largeMonolithicJavaProject"], iterationMatcher = '.*parallel false.*'),
    ])
    @Unroll
    def "up-to-date assemble with local build cache enabled (parallel #parallel)"() {
        given:
        runner.tasksToRun = ['assemble']
        runner.targetVersions = ["6.8-20201113131108+0000"]
        runner.minimumBaseVersion = "3.5"
        runner.args += ["-Dorg.gradle.parallel=$parallel", "-D${StartParameterBuildOptions.BuildCacheOption.GRADLE_PROPERTY}=true"]
        runner.addBuildMutator { invocationSettings ->
            new ClearBuildCacheMutator(invocationSettings.getGradleUserHome(), AbstractCleanupMutator.CleanupSchedule.SCENARIO)
        }

        when:
        def result = runner.run()

        then:
        result.assertCurrentVersionHasNotRegressed()

        where:
        parallel << [true, false]
    }
}
