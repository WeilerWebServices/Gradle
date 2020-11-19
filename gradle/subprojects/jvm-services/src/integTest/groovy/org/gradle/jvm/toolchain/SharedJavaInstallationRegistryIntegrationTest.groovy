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

package org.gradle.jvm.toolchain

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.AvailableJavaHomes
import spock.lang.IgnoreIf

class SharedJavaInstallationRegistryIntegrationTest extends AbstractIntegrationSpec {

    def "installation registry has no installations without environment setup or auto-detection"() {
        buildFile << """
            import org.gradle.jvm.toolchain.internal.SharedJavaInstallationRegistry;

            abstract class ShowPlugin implements Plugin<Project> {
                @Inject
                abstract SharedJavaInstallationRegistry getRegistry()

                void apply(Project project) {
                    project.tasks.register("show") {
                        println "installations:" + registry.listInstallations()
                    }
                }
            }

            apply plugin: ShowPlugin
        """

        when:
        result = executer
            .withArgument("-Porg.gradle.java.installations.auto-detect=false")
            .withTasks("show")
            .run()

        then:
        outputContains("installations:[]")
    }

    @IgnoreIf({ AvailableJavaHomes.availableJvms.size() < 2 })
    def "installation registry is populated by environment"() {
        def firstJavaHome = AvailableJavaHomes.availableJvms[0].javaHome.absolutePath
        def secondJavaHome = AvailableJavaHomes.availableJvms[1].javaHome.absolutePath

        buildFile << """
            import org.gradle.jvm.toolchain.internal.SharedJavaInstallationRegistry;

            abstract class ShowPlugin implements Plugin<Project> {
                @Inject
                abstract SharedJavaInstallationRegistry getRegistry()

                void apply(Project project) {
                    project.tasks.register("show") {
                       registry.listInstallations().each { println it.location }
                    }
                }
            }

            apply plugin: ShowPlugin
        """

        when:
        result = executer
            .withEnvironmentVars([JDK1: "/unknown/env", JDK2: firstJavaHome])
            .withArgument("-Porg.gradle.java.installations.paths=/unknown/path," + secondJavaHome)
            .withArgument("-Porg.gradle.java.installations.fromEnv=JDK1,JDK2")
            .withArgument("--info")
            .withTasks("show")
            .run()
        then:
        outputContains("${File.separator}unknown${File.separator}path' (system property 'org.gradle.java.installations.paths') used for java installations does not exist")
        outputContains("${File.separator}unknown${File.separator}env' (environment variable 'JDK1') used for java installations does not exist")
        outputContains(firstJavaHome)
        outputContains(secondJavaHome)

        when:
        result = executer
            .withEnvironmentVars([JDK1: "/unknown/env", JDK2: firstJavaHome])
            .withArgument("-Porg.gradle.java.installations.paths=/other/path," + secondJavaHome)
            .withArgument("-Porg.gradle.java.installations.fromEnv=JDK1,JDK2")
            .withTasks("show")
            .run()
        then:
        outputContains("${File.separator}other${File.separator}path' (system property 'org.gradle.java.installations.paths') used for java installations does not exist")
        outputContains("${File.separator}unknown${File.separator}env' (environment variable 'JDK1') used for java installations does not exist")
        outputContains(firstJavaHome)
        outputContains(secondJavaHome)
    }

}
