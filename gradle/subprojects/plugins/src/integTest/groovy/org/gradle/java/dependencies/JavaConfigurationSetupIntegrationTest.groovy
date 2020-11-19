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

package org.gradle.java.dependencies

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import spock.lang.Unroll

class JavaConfigurationSetupIntegrationTest extends AbstractIntegrationSpec {

    static final VALID = "_VALID"
    static final FORBIDDEN = "_FORBIDDEN"
    static final DOES_NOT_EXIST = "_DOES_NOT_EXIST"

    static forbidden(String marker) {
        marker == FORBIDDEN
    }

    static doesNotExist(String marker) {
        marker == DOES_NOT_EXIST
    }

    static valid(String marker) {
        marker == VALID
    }

    static deprecated(String alternatives) {
        !(alternatives in [VALID, FORBIDDEN, DOES_NOT_EXIST])
    }

    @Unroll
    def "the #configuration configuration is setup correctly for dependency declaration in the #plugin plugin"() {
        given:
        buildFile << """
            plugins { id '$plugin' }
            dependencies {
                $configuration 'some:module:1.0'
            }
        """

        when:
        if (deprecated(alternatives)) {
            executer.expectDeprecationWarning()
        }
        if (forbidden(alternatives) || doesNotExist(alternatives)) {
            fails 'help'
        } else {
            succeeds 'help'
        }

        then:
        !deprecated(alternatives)   || output.contains("The $configuration configuration has been deprecated for dependency declaration. This will fail with an error in Gradle 7.0. Please use the $alternatives configuration instead.")
        !valid(alternatives)        || !output.contains("> Configure project :")
        !doesNotExist(alternatives) || errorOutput.contains("Could not find method $configuration() for arguments [some:module:1.0] on object of type org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler")

        where:
        plugin         | configuration                  | alternatives

        'java'         | 'compile'                      | "implementation"
        'java'         | 'runtime'                      | "runtimeOnly"
        'java'         | 'compileOnly'                  | VALID
        'java'         | 'runtimeOnly'                  | VALID
        'java'         | 'implementation'               | VALID
        'java'         | 'apiElements'                  | "implementation or compileOnly"
        'java'         | 'runtimeElements'              | "implementation or compileOnly or runtimeOnly"
        'java'         | 'compileClasspath'             | "implementation or compileOnly"
        'java'         | 'runtimeClasspath'             | "implementation or compileOnly or runtimeOnly"
        'java'         | 'annotationProcessor'          | VALID
        'java'         | 'api'                          | DOES_NOT_EXIST
        'java'         | 'compileOnlyApi'               | DOES_NOT_EXIST

        'java-library' | 'compile'                      | "implementation or api"
        'java-library' | 'runtime'                      | "runtimeOnly"
        'java-library' | 'compileOnly'                  | VALID
        'java-library' | 'runtimeOnly'                  | VALID
        'java-library' | 'implementation'               | VALID
        'java-library' | 'apiElements'                  | "implementation or api or compileOnly"
        'java-library' | 'runtimeElements'              | "implementation or api or compileOnly or runtimeOnly"
        'java-library' | 'compileClasspath'             | "implementation or api or compileOnly"
        'java-library' | 'runtimeClasspath'             | "implementation or api or compileOnly or runtimeOnly"
        'java-library' | 'annotationProcessor'          | VALID
        'java-library' | 'api'                          | VALID
        'java-library' | 'compileOnlyApi'               | VALID
    }

    @Unroll
    def "the #configuration configuration is setup correctly for consumption in the #plugin plugin"() {
        when:
        settingsFile.text = "include 'sub'"
        buildFile.text = """
            project(':sub') { apply plugin: '$plugin' }
            configurations { root { attributes.attribute Attribute.of('artifactType', String), 'jar' } }
            dependencies {
                root project(path: ':sub', configuration: '$configuration')
            }
            task resolve {
                doLast {
                    configurations.root.files
                }
            }
        """
        if (deprecated(alternatives)) {
            executer.expectDeprecationWarning()
        }
        if (forbidden(alternatives) || doesNotExist(alternatives)) {
            fails 'resolve'
        } else {
            succeeds 'resolve'
        }

        then:
        !deprecated(alternatives)   || output.contains("The $configuration configuration has been deprecated for consumption. This will fail with an error in Gradle 7.0. Please use attributes to consume the ${alternatives} configuration instead.")
        !valid(alternatives)        || output.contains("> Task :resolve\n\n")
        !forbidden(alternatives)    || errorOutput.contains("Selected configuration '$configuration' on 'project :sub' but it can't be used as a project dependency because it isn't intended for consumption by other components.")
        !doesNotExist(alternatives) || errorOutput.contains("Project : declares a dependency from configuration 'root' to configuration '$configuration' which is not declared in the descriptor for project :sub.")

        where:
        plugin         | configuration                  | alternatives

        'java'         | 'compile'                      | "apiElements"
        'java'         | 'runtime'                      | "runtimeElements"
        'java'         | 'compileOnly'                  | "apiElements"
        'java'         | 'runtimeOnly'                  | FORBIDDEN
        'java'         | 'implementation'               | FORBIDDEN
        'java'         | 'runtimeElements'              | VALID
        'java'         | 'apiElements'                  | VALID
        'java'         | 'compileClasspath'             | FORBIDDEN
        'java'         | 'runtimeClasspath'             | FORBIDDEN
        'java'         | 'annotationProcessor'          | FORBIDDEN
        'java'         | 'api'                          | DOES_NOT_EXIST
        'java'         | 'compileOnlyApi'               | DOES_NOT_EXIST

        'java-library' | 'compile'                      | "apiElements"
        'java-library' | 'runtime'                      | "runtimeElements"
        'java-library' | 'compileOnly'                  | "apiElements"
        'java-library' | 'runtimeOnly'                  | FORBIDDEN
        'java-library' | 'implementation'               | FORBIDDEN
        'java-library' | 'runtimeElements'              | VALID
        'java-library' | 'apiElements'                  | VALID
        'java-library' | 'compileClasspath'             | FORBIDDEN
        'java-library' | 'runtimeClasspath'             | FORBIDDEN
        'java-library' | 'annotationProcessor'          | FORBIDDEN
        'java-library' | 'api'                          | FORBIDDEN
        'java-library' | 'compileOnlyApi'               | FORBIDDEN
    }

    @Unroll
    def "the #configuration configuration is setup correctly for resolution in the #plugin plugin"() {
        given:
        buildFile << """
            plugins { id '$plugin' }
            task resolve {
                doLast {
                    configurations.${configuration}.files
                }
            }
        """

        when:
        if (deprecated(alternatives)) {
            executer.expectDeprecationWarning()
        }
        if (forbidden(alternatives) || doesNotExist(alternatives)) {
            fails 'resolve'
        } else {
            succeeds 'resolve'
        }

        then:
        !deprecated(alternatives)   || output.contains("The $configuration configuration has been deprecated for resolution. This will fail with an error in Gradle 7.0. Please resolve the ${alternatives} configuration instead.")
        !valid(alternatives)        || output.contains("> Task :resolve\n\n")
        !forbidden(alternatives)    || errorOutput.contains("Resolving dependency configuration '$configuration' is not allowed as it is defined as 'canBeResolved=false'.\nInstead, a resolvable ('canBeResolved=true') dependency configuration that extends '$configuration' should be resolved.")
        !doesNotExist(alternatives) || errorOutput.contains("Could not get unknown property '$configuration' for configuration container of type org.gradle.api.internal.artifacts.configurations.DefaultConfigurationContainer.")

        where:
        plugin         | configuration                  | alternatives

        'java'         | 'compile'                      | "compileClasspath"
        'java'         | 'runtime'                      | "runtimeClasspath"
        'java'         | 'compileOnly'                  | "compileClasspath"
        'java'         | 'runtimeOnly'                  | FORBIDDEN
        'java'         | 'implementation'               | FORBIDDEN
        'java'         | 'runtimeElements'              | FORBIDDEN
        'java'         | 'apiElements'                  | FORBIDDEN
        'java'         | 'compileClasspath'             | VALID
        'java'         | 'runtimeClasspath'             | VALID
        'java'         | 'annotationProcessor'          | VALID
        'java'         | 'api'                          | DOES_NOT_EXIST
        'java'         | 'compileOnlyApi'               | DOES_NOT_EXIST

        'java-library' | 'compile'                      | "compileClasspath"
        'java-library' | 'runtime'                      | "runtimeClasspath"
        'java-library' | 'compileOnly'                  | "compileClasspath"
        'java-library' | 'runtimeOnly'                  | FORBIDDEN
        'java-library' | 'implementation'               | FORBIDDEN
        'java-library' | 'runtimeElements'              | FORBIDDEN
        'java-library' | 'apiElements'                  | FORBIDDEN
        'java-library' | 'compileClasspath'             | VALID
        'java-library' | 'runtimeClasspath'             | VALID
        'java-library' | 'annotationProcessor'          | VALID
        'java-library' | 'api'                          | FORBIDDEN
        'java-library' | 'compileOnlyApi'               | FORBIDDEN
    }

    def "compileOnyApi dependency is part of compileOnly"() {
        given:
        mavenRepo.module("org", "a").publish()

        buildFile << """
            plugins {
               id('java-library')
            }
            repositories {
                maven { url '$mavenRepo.uri' }
            }
            dependencies {
               compileOnlyApi 'org:a:1.0'
            }
            configurations.create('compileOnlyPath') {
               extendsFrom(configurations.compileOnly)
            }
            tasks.register('resolveCompileOnly') {
              doLast { println(configurations.compileOnlyPath.collect { it.name }) }
            }
        """

        when:
        succeeds 'resolveCompileOnly'

        then:
        outputContains('[a-1.0.jar]')
    }
}
