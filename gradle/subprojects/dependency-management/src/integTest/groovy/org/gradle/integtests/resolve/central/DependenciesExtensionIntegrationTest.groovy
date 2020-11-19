/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.integtests.resolve.central

import org.gradle.integtests.fixtures.UnsupportedWithConfigurationCache
import spock.lang.Unroll

class DependenciesExtensionIntegrationTest extends AbstractCentralDependenciesIntegrationTest {

    @Unroll
    @UnsupportedWithConfigurationCache(because = "the test uses an extension directly in the task body")
    def "dependencies declared in settings trigger the creation of an extension (notation=#notation)"() {
        settingsFile << """
            dependencyResolutionManagement {
                versionCatalog("libs") {
                    $notation
                }
            }
        """

        buildFile << """
            apply plugin: 'java-library'

            tasks.register("verifyExtension") {
                doLast {
                    def lib = libs.foo
                    assert lib instanceof Provider
                    def dep = lib.get()
                    assert dep instanceof MinimalExternalModuleDependency
                    assert dep.module.group == 'org.gradle.test'
                    assert dep.module.name == 'lib'
                    assert dep.versionConstraint.requiredVersion == '1.0'
                }
            }
        """

        when:
        run 'verifyExtension'

        then:
        operations.hasOperation("Executing generation of dependency accessors for libs")

        when: "no change in settings"
        run 'verifyExtension'

        then: "extension is not regenerated"
        !operations.hasOperation("Executing generation of dependency accessors for libs")

        when: "adding a library to the model"
        settingsFile << """
            dependencyResolutionManagement {
                versionCatalog("libs") {
                    alias("bar") to "org.gradle.test:bar:1.0"
                }
            }
        """
        run 'verifyExtension'
        then: "extension is regenerated"
        operations.hasOperation("Executing generation of dependency accessors for libs")

        when: "updating a version in the model"
        settingsFile.text = settingsFile.text.replace('org.gradle.test:bar:1.0', 'org.gradle.test:bar:1.1')
        run 'verifyExtension'

        then: "extension is not regenerated"
        !operations.hasOperation("Executing generation of dependency accessors for libs")
        outputContains 'Type-safe dependency accessors is an incubating feature.'

        where:
        notation << [
            'alias("foo").to("org.gradle.test:lib:1.0")',
            'alias("foo").to("org.gradle.test", "lib").version { require "1.0" }',
            'alias("foo").to("org.gradle.test", "lib").version("1.0")'
        ]
    }

    def "can use the generated extension to declare a dependency"() {
        settingsFile << """
            dependencyResolutionManagement {
                versionCatalog("libs") {
                    alias("myLib").to("org.gradle.test", "lib").version {
                        require "1.0"
                    }
                }
            }
        """
        def lib = mavenHttpRepo.module("org.gradle.test", "lib", "1.0").publish()
        buildFile << """
            apply plugin: 'java-library'

            dependencies {
                implementation libs.myLib
            }
        """

        when:
        lib.pom.expectGet()
        lib.artifact.expectGet()

        then:
        run ':checkDeps'

        then:
        resolve.expectGraph {
            root(":", ":test:") {
                module('org.gradle.test:lib:1.0')
            }
        }
    }

    def "can use the generated extension to declare a dependency constraint"() {
        settingsFile << """
            dependencyResolutionManagement {
                versionCatalog("libs") {
                    alias("myLib").to("org.gradle.test", "lib").version {
                        require "1.0"
                    }
                }
            }
        """
        def lib = mavenHttpRepo.module("org.gradle.test", "lib", "1.0").publish()
        buildFile << """
            apply plugin: 'java-library'

            dependencies {
                implementation "org.gradle.test:lib" // intentional!
                constraints {
                    implementation(libs.myLib)
                }
            }
        """

        when:
        lib.pom.expectGet()
        lib.artifact.expectGet()

        then:
        run ':checkDeps'

        then:
        resolve.expectGraph {
            root(":", ":test:") {
                constraint('org.gradle.test:lib:1.0')
                edge('org.gradle.test:lib', 'org.gradle.test:lib:1.0')
            }
        }
    }

    def "can use the generated extension to declare a dependency and override the version"() {
        settingsFile << """
            dependencyResolutionManagement {
                versionCatalog("libs") {
                    alias("myLib").to("org.gradle.test", "lib").version {
                        require "1.0"
                    }
                }
            }
        """
        def lib = mavenHttpRepo.module("org.gradle.test", "lib", "1.1").publish()
        buildFile << """
            apply plugin: 'java-library'

            dependencies {
                implementation(libs.myLib) {
                    version {
                        require '1.1'
                    }
                }
            }
        """

        when:
        lib.pom.expectGet()
        lib.artifact.expectGet()

        then:
        run ':checkDeps'

        then:
        resolve.expectGraph {
            root(":", ":test:") {
                module('org.gradle.test:lib:1.1')
            }
        }
    }

    def "can use the generated extension to declare a dependency constraint and override the version"() {
        settingsFile << """
            dependencyResolutionManagement {
                versionCatalog("libs") {
                    alias("myLib").to("org.gradle.test", "lib").version {
                        require "1.0"
                    }
                }
            }
        """
        def lib = mavenHttpRepo.module("org.gradle.test", "lib", "1.1").publish()
        buildFile << """
            apply plugin: 'java-library'

            dependencies {
                implementation "org.gradle.test:lib" // intentional!
                constraints {
                    implementation(libs.myLib) {
                        version {
                            require '1.1'
                        }
                    }
                }
            }
        """

        when:
        lib.pom.expectGet()
        lib.artifact.expectGet()

        then:
        run ':checkDeps'

        then:
        resolve.expectGraph {
            root(":", ":test:") {
                constraint('org.gradle.test:lib:1.1')
                edge('org.gradle.test:lib', 'org.gradle.test:lib:1.1')
            }
        }
    }

    void "can add several dependencies at once using a bundle"() {
        settingsFile << """
            dependencyResolutionManagement {
                versionCatalog("libs") {
                    alias("lib").to("org.gradle.test", "lib").version {
                        require "1.0"
                    }
                    alias("lib2").to("org.gradle.test:lib2:1.0")
                    bundle("myBundle", ["lib", "lib2"])
                }
            }
        """
        def lib = mavenHttpRepo.module("org.gradle.test", "lib", "1.0").publish()
        def lib2 = mavenHttpRepo.module("org.gradle.test", "lib2", "1.0").publish()
        buildFile << """
            apply plugin: 'java-library'

            dependencies {
                implementation(libs.bundles.myBundle)
            }
        """

        when:
        lib.pom.expectGet()
        lib.artifact.expectGet()
        lib2.pom.expectGet()
        lib2.artifact.expectGet()

        then:
        run ':checkDeps'

        then:
        resolve.expectGraph {
            root(":", ":test:") {
                module('org.gradle.test:lib:1.0')
                module('org.gradle.test:lib2:1.0')
            }
        }
    }

    void "overriding the version of a bundle overrides the version of all dependencies of the bundle"() {
        settingsFile << """
            dependencyResolutionManagement {
                versionCatalog("libs") {
                    alias("lib").to("org.gradle.test", "lib").version {
                        require "1.0"
                    }
                    alias("lib2").to("org.gradle.test:lib2:1.0")
                    bundle("myBundle", ["lib", "lib2"])
                }
            }
        """
        def lib = mavenHttpRepo.module("org.gradle.test", "lib", "1.1").publish()
        def lib2 = mavenHttpRepo.module("org.gradle.test", "lib2", "1.1").publish()
        buildFile << """
            apply plugin: 'java-library'

            dependencies {
                implementation(libs.bundles.myBundle) {
                    version {
                        require '1.1'
                    }
                }
            }
        """

        when:
        lib.pom.expectGet()
        lib.artifact.expectGet()
        lib2.pom.expectGet()
        lib2.artifact.expectGet()

        then:
        run ':checkDeps'

        then:
        resolve.expectGraph {
            root(":", ":test:") {
                module('org.gradle.test:lib:1.1')
                module('org.gradle.test:lib2:1.1')
            }
        }
    }

    def "can configure a different libraries extension"() {
        settingsFile << """
            dependencyResolutionManagement {
                versionCatalog("libraries") {
                    alias("myLib").to("org.gradle.test", "lib").version {
                        require "1.0"
                    }
                }
            }
        """
        def lib = mavenHttpRepo.module("org.gradle.test", "lib", "1.0").publish()
        buildFile << """
            apply plugin: 'java-library'

            dependencies {
                implementation libraries.myLib
            }
        """

        when:
        lib.pom.expectGet()
        lib.artifact.expectGet()

        then:
        run ':checkDeps'

        then:
        resolve.expectGraph {
            root(":", ":test:") {
                module('org.gradle.test:lib:1.0')
            }
        }
    }

    def "can configure multiple libraries extension"() {
        settingsFile << """
            dependencyResolutionManagement {
                versionCatalog("libraries") {
                    alias("myLib").to("org.gradle.test", "lib").version {
                        require "1.0"
                    }
                }
                versionCatalog("other") {
                    alias("great").to("org.gradle.test", "lib2").version {
                        require "1.1"
                    }
                }
            }
        """
        def lib = mavenHttpRepo.module("org.gradle.test", "lib", "1.0").publish()
        def lib2 = mavenHttpRepo.module("org.gradle.test", "lib2", "1.1").publish()
        buildFile << """
            apply plugin: 'java-library'

            dependencies {
                implementation libraries.myLib
                implementation other.great
            }
        """

        when:
        lib.pom.expectGet()
        lib.artifact.expectGet()
        lib2.pom.expectGet()
        lib2.artifact.expectGet()

        then:
        run ':checkDeps'

        then:
        resolve.expectGraph {
            root(":", ":test:") {
                module('org.gradle.test:lib:1.0')
                module('org.gradle.test:lib2:1.1')
            }
        }
    }

    // makes sure the extension with a different name is created even if a
    // different one with same contents exists
    def "can configure multiple libraries extension with same contents"() {
        settingsFile << """
            dependencyResolutionManagement {
                versionCatalog("libraries") {
                    alias("myLib").to("org.gradle.test", "lib").version {
                        require "1.0"
                    }
                }
                versionCatalog("other") {
                    alias("myLib").to("org.gradle.test", "lib").version {
                        require "1.0"
                    }
                }
            }
        """
        def lib = mavenHttpRepo.module("org.gradle.test", "lib", "1.0").publish()
        buildFile << """
            apply plugin: 'java-library'

            dependencies {
                implementation libraries.myLib
                implementation other.myLib
            }
        """

        when:
        lib.pom.expectGet()
        lib.artifact.expectGet()

        then:
        run ':checkDeps'

        then:
        resolve.expectGraph {
            root(":", ":test:") {
                module('org.gradle.test:lib:1.0')
            }
        }
    }

    def "extension can be used in any subproject"() {
        settingsFile << """
            dependencyResolutionManagement {
                versionCatalog("libs") {
                    alias("lib").to("org.gradle.test:lib:1.0")
                }
            }
            include 'other'
        """
        def lib = mavenHttpRepo.module("org.gradle.test", "lib", "1.0").publish()
        buildFile << """
            apply plugin: 'java-library'

            dependencies {
                implementation project(":other")
            }
        """

        file("other/build.gradle") << """
            plugins {
                id 'java-library'
            }

            dependencies {
                implementation libs.lib
            }
        """

        when:
        lib.pom.expectGet()
        lib.artifact.expectGet()

        then:
        run ':checkDeps'

        then:
        resolve.expectGraph {
            root(":", ":test:") {
                project(":other", "test:other:") {
                    module('org.gradle.test:lib:1.0')
                }
            }
        }
    }

    def "libraries extension is not visible in buildSrc"() {
        settingsFile << """
            dependencyResolutionManagement {
                versionCatalog("libs") {
                    alias("lib").to("org.gradle.test:lib:1.0")
                }
            }
        """
        file("buildSrc/build.gradle") << """
            dependencies {
                classpath libs.lib
            }
        """

        when:
        fails ':help'

        then: "extension is not generated if there are no libraries defined"
        failure.assertHasCause("Could not get unknown property 'libs' for object of type org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler")
    }

    def "buildSrc and main project have different libraries extensions"() {
        settingsFile << """
            dependencyResolutionManagement {
                versionCatalog("libs") {
                    alias("lib").to("org.gradle.test:lib:1.0")
                }
            }
        """
        file("buildSrc/build.gradle") << """
            repositories {
                maven { url "${mavenHttpRepo.uri}" }
            }

            dependencies {
                implementation libs.buildSrcLib
            }
        """
        file("buildSrc/settings.gradle") << """
            dependencyResolutionManagement {
                versionCatalog("libs") {
                    alias("build-src-lib").to("org.gradle.test:buildsrc-lib:1.0")
                }
            }
        """
        buildFile << """
            apply plugin: 'java-library'

            dependencies {
                implementation libs.lib
            }
        """

        def buildSrcLib = mavenHttpRepo.module('org.gradle.test', 'buildsrc-lib', '1.0').publish()
        def lib = mavenHttpRepo.module('org.gradle.test', 'lib', '1.0').publish()

        when:
        buildSrcLib.pom.expectGet()
        buildSrcLib.artifact.expectGet()
        lib.pom.expectGet()
        lib.artifact.expectGet()

        succeeds ':checkDeps'

        then:
        resolve.expectGraph {
            root(":", ":test:") {
                module('org.gradle.test:lib:1.0')
            }
        }
    }

    def "included builds use their own libraries extension"() {
        file("included/build.gradle") << """
            plugins {
                id 'java-library'
            }

            group = 'com.acme'
            version = 'zloubi'

            dependencies {
                implementation libs.fromIncluded
            }
        """
        file("included/settings.gradle") << """
            rootProject.name = 'included'

            dependencyResolutionManagement {
                repositories {
                    maven { url "${mavenHttpRepo.uri}" }
                }
                versionCatalog("libs") {
                    alias('from-included').to('org.gradle.test:other:1.1')
                }
            }
        """

        settingsFile << """
            includeBuild 'included'
        """

        buildFile << """
            apply plugin: 'java-library'

            dependencies {
                implementation 'com.acme:included:1.0'
            }
        """
        def lib = mavenHttpRepo.module('org.gradle.test', 'other', '1.1').publish()

        when:
        lib.pom.expectGet()
        lib.artifact.expectGet()
        succeeds ':checkDeps'

        then:
        resolve.expectGraph {
            root(":", ":test:") {
                edge("com.acme:included:1.0", "project :included", "com.acme:included:zloubi") {
                    compositeSubstitute()
                    configuration = "runtimeElements"
                    module('org.gradle.test:other:1.1')
                }
            }
        }
    }

    def "can declare a version with a name and reference it"() {
        settingsFile << """
            dependencyResolutionManagement {
                versionCatalog("libs") {
                    version("myVersion") {
                        require "1.0"
                    }
                    alias("myLib").to("org.gradle.test", "lib").versionRef("myVersion")
                    alias("myOtherLib").to("org.gradle.test", "lib2").versionRef("myOtherVersion")
                    version("myOtherVersion") {
                        // intentionnally declared AFTER
                        strictly "1.1"
                    }
                }
            }
        """
        def lib = mavenHttpRepo.module("org.gradle.test", "lib", "1.0").publish()
        def lib2 = mavenHttpRepo.module("org.gradle.test", "lib2", "1.1").publish()
        buildFile << """
            apply plugin: 'java-library'

            dependencies {
                implementation libs.myLib
                implementation libs.myOtherLib
            }
        """

        when:
        lib.pom.expectGet()
        lib.artifact.expectGet()
        lib2.pom.expectGet()
        lib2.artifact.expectGet()

        then:
        run ':checkDeps'

        then:
        resolve.expectGraph {
            root(":", ":test:") {
                module('org.gradle.test:lib:1.0')
                edge('org.gradle.test:lib2:{strictly 1.1}', 'org.gradle.test:lib2:1.1')
            }
        }
    }

    def "multiple libraries can use the same version reference"() {
        settingsFile << """
            dependencyResolutionManagement {
                versionCatalog("libs") {
                    def myVersion = version("myVersion") {
                        require "1.0"
                    }
                    alias("myLib").to("org.gradle.test", "lib").versionRef("myVersion")
                    alias("myOtherLib").to("org.gradle.test", "lib2").versionRef(myVersion)
                }
            }
        """
        def lib = mavenHttpRepo.module("org.gradle.test", "lib", "1.0").publish()
        def lib2 = mavenHttpRepo.module("org.gradle.test", "lib2", "1.0").publish()
        buildFile << """
            apply plugin: 'java-library'

            dependencies {
                implementation libs.myLib
                implementation libs.myOtherLib
            }
        """

        when:
        lib.pom.expectGet()
        lib.artifact.expectGet()
        lib2.pom.expectGet()
        lib2.artifact.expectGet()

        then:
        run ':checkDeps'

        then:
        resolve.expectGraph {
            root(":", ":test:") {
                module('org.gradle.test:lib:1.0')
                module('org.gradle.test:lib2:1.0')
            }
        }
    }

    @Unroll
    def "can generate a version accessor and use it in a build script"() {
        settingsFile << """
            dependencyResolutionManagement {
                versionCatalog("libs") {
                    version("libVersion") {
                        $notation
                    }
                }
            }
        """
        def lib = mavenHttpRepo.module("org.gradle.test", "lib", "1.0").publish()
        buildFile << """
            apply plugin: 'java-library'

            dependencies {
                implementation "org.gradle.test:lib:\${libs.versions.libVersion.get()}"
            }
        """

        when:
        lib.pom.expectGet()
        lib.artifact.expectGet()

        then:
        run ':checkDeps'

        then:
        resolve.expectGraph {
            root(":", ":test:") {
                // always 1.0 because calling `getLibVersion` will always loose the rich aspect
                // of the version model
                module("org.gradle.test:lib:1.0")
            }
        }

        where:
        notation << [
            "require '1.0'",
            "strictly '1.0'",
            "prefer '1.0'"
        ]

    }
}
