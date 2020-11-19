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

package org.gradle.performance.experiment.java

import org.gradle.internal.os.OperatingSystem
import org.gradle.performance.AbstractCrossBuildPerformanceTest
import org.gradle.performance.annotations.RunFor
import org.gradle.performance.annotations.Scenario
import org.gradle.performance.results.BaselineVersion
import org.gradle.performance.results.CrossBuildPerformanceResults

import static org.gradle.performance.annotations.ScenarioType.SLOW
import static org.gradle.performance.results.OperatingSystem.LINUX
import static org.gradle.performance.results.OperatingSystem.WINDOWS

@RunFor(
    @Scenario(type = SLOW, operatingSystems = [LINUX, WINDOWS], testProjects = ["largeJavaMultiProject"])
)
class JavaLibraryPluginPerformanceTest extends AbstractCrossBuildPerformanceTest {

    def "java-library vs java"() {
        def javaLibraryRuns = "java-library-plugin"
        def javaRuns = "java-plugin"
        def compileClasspathPackaging = OperatingSystem.current().windows

        given:
        runner.testGroup = "java plugins"
        runner.buildSpec {
            warmUpCount = 2
            invocationCount = 3
            displayName(javaLibraryRuns)
            invocation {
                tasksToRun("clean", "classes")
                args("-PcompileConfiguration", "-Dorg.gradle.java.compile-classpath-packaging=$compileClasspathPackaging")
            }
        }
        runner.baseline {
            warmUpCount = 2
            invocationCount = 3
            displayName(javaRuns)
            invocation {
                tasksToRun("clean", "classes")
                args("-PcompileConfiguration", "-PnoJavaLibraryPlugin")
            }
        }

        when:
        def results = runner.run()

        then:
        def javaResults = buildBaselineResults(results, javaRuns)
        def javaLibraryResults = results.buildResult(javaLibraryRuns)
        def speedStats = javaResults.getSpeedStatsAgainst(javaLibraryResults.name, javaLibraryResults)
        println(speedStats)
        if (javaResults.significantlyFasterThan(javaLibraryResults, 0.95)) {
            throw new AssertionError(speedStats)
        }
    }

    private static BaselineVersion buildBaselineResults(CrossBuildPerformanceResults results, String name) {
        def baselineResults = new BaselineVersion(name)
        baselineResults.results.name = name
        baselineResults.results.addAll(results.buildResult(name))
        return baselineResults
    }
}
