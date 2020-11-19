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

import org.gradle.integtests.fixtures.executer.GradleContextualExecuter
import org.gradle.testkit.runner.fixtures.PluginUnderTest
import spock.lang.IgnoreIf

@IgnoreIf({ GradleContextualExecuter.embedded }) // These tests run builds that themselves run a build in a test worker with 'gradleTestKit()' dependency, which needs to pick up Gradle modules from a real distribution
class GradleRunnerConventionalPluginClasspathInjectionEndUserIntegrationTest extends BaseTestKitEndUserIntegrationTest {

    def plugin = new PluginUnderTest(testDirectory)

    def setup() {
        buildFile << """
            plugins {
                id "org.gradle.java-gradle-plugin"
                id "org.gradle.groovy"
            }
            ${jcenterRepository()}
            dependencies {
                testImplementation('org.spockframework:spock-core:1.0-groovy-2.4') {
                    exclude module: 'groovy-all'
                }
            }
        """

        plugin.writeSourceFiles()

        file("src/test/groovy/Test.groovy") << """
            import org.gradle.testkit.runner.GradleRunner
            import static org.gradle.testkit.runner.TaskOutcome.*
            import org.junit.Rule
            import org.junit.rules.TemporaryFolder
            import spock.lang.Specification

            class Test extends Specification {

                @Rule TemporaryFolder testProjectDir = new TemporaryFolder()

                def "execute helloWorld task"() {
                    given:
                    testProjectDir.newFile('settings.gradle') << "rootProject.name = 'plugin-test'"
                    testProjectDir.newFile('build.gradle') << '''$plugin.useDeclaration'''

                    when:
                    def result = GradleRunner.create()
                        .withProjectDir(testProjectDir.root)
                        .withArguments('helloWorld')
                        .withPluginClasspath()
                        .withDebug($debug)
                        .build()

                    then:
                    noExceptionThrown()
                }
            }
        """
    }

    def "can test plugin and custom task as external files by using default conventions from Java Gradle plugin development plugin"() {
        expect:
        succeeds 'test'
        executedAndNotSkipped ':test'
    }

    def "can override plugin metadata location"() {
        when:
        buildFile << """
            pluginUnderTestMetadata {
                outputDirectory = file('build/testkit/manifest')
            }
        """

        then:
        succeeds 'test'
        executedAndNotSkipped ':test'
    }

    def "can use custom source set"() {
        when:
        file("src/test/groovy/Test.groovy").moveToDirectory(file("src/functionalTest/groovy"))
        buildFile << """
            sourceSets {
                functionalTest {}
            }

            configurations {
                functionalTestCompile.extendsFrom testImplementation
                functionalTestRuntime.extendsFrom testRuntime
            }

            task functionalTest(type: Test) {
                testClassesDirs = sourceSets.functionalTest.output.classesDirs
                classpath = sourceSets.functionalTest.runtimeClasspath
            }

            gradlePlugin {
                testSourceSets sourceSets.functionalTest
            }
        """

        then:
        succeeds 'functionalTest'
        executedAndNotSkipped ':functionalTest'

        when:
        // Changes source but not class file
        plugin.pluginClassSourceFile() << """
            // Comment
        """.stripIndent()

        then:
        succeeds 'functionalTest'
        executedAndNotSkipped ":compileGroovy"
        skipped ':functionalTest'

        when:
        // Changes line numbers, so changes class file
        plugin.pluginClassSourceFile().text = "\n\n\n" + plugin.pluginClassSourceFile().text

        then:
        succeeds 'functionalTest'
        executedAndNotSkipped ":functionalTest"
    }

}
