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

import org.gradle.api.JavaVersion
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.AvailableJavaHomes
import org.gradle.integtests.fixtures.ToBeFixedForConfigurationCache
import org.gradle.integtests.fixtures.executer.GradleExecuter
import org.gradle.internal.jvm.Jvm
import org.gradle.internal.os.OperatingSystem
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition
import org.junit.Assume
import spock.lang.IgnoreIf
import spock.lang.Unroll

class JavaInstallationRegistryIntegrationTest extends AbstractIntegrationSpec {

    def setup() {
        expectDocumentedDeprecationWarning()
    }

    private GradleExecuter expectDocumentedDeprecationWarning() {
        executer.expectDocumentedDeprecationWarning("Using JavaInstallationRegistry to detect Java installations has been deprecated. This is scheduled to be removed in Gradle 7.0. Consider using Java Toolchains instead. See https://docs.gradle.org/current/userguide/toolchains.html for more details.")
    }

    def "plugin can query information about the current JVM"() {
        taskTypeShowsJavaInstallationDetails()
        pluginShowsCurrentJvm()

        when:
        run("show")

        then:
        outputContains("install dir = ${Jvm.current().javaHome}")
        outputContains("java version = ${Jvm.current().javaVersion}")
        outputContains("java executable = ${Jvm.current().javaExecutable}")
        outputContains("JDK? = true")
        outputContains("javac executable = ${Jvm.current().javacExecutable}")
        outputContains("javadoc executable = ${Jvm.current().javadocExecutable}")
    }

    @Requires(TestPrecondition.JDK8)
    def "plugin can query tools classpath for the current JVM on Java 8"() {
        taskTypeShowsJavaInstallationDetails()
        pluginShowsCurrentJvm()

        when:
        run("show")

        then:
        outputContains("tools classpath = [${Jvm.current().toolsJar}]")
    }

    @Requires(TestPrecondition.JDK9_OR_LATER)
    def "plugin can query tools classpath for the current JVM on Java 9+"() {
        taskTypeShowsJavaInstallationDetails()
        pluginShowsCurrentJvm()

        when:
        run("show")

        then:
        outputContains("tools classpath = []")
    }

    @IgnoreIf({ AvailableJavaHomes.differentVersion == null })
    def "plugin can query information about another JDK install"() {
        def jvm = AvailableJavaHomes.differentVersion

        taskTypeShowsJavaInstallationDetails()
        buildFile << """
            task show(type: ShowTask) {
                installation = services.get(JavaInstallationRegistry).installationForDirectory(project.layout.projectDirectory.dir("${jvm.javaHome.toURI()}"))
            }
        """

        when:
        run("show")

        then:
        outputContains("install dir = ${jvm.javaHome}")
        outputContains("java version = ${jvm.javaVersion}")
        outputContains("java executable = ${jvm.javaExecutable}")
        outputContains("JDK? = true")
        outputContains("javac executable = ${jvm.javacExecutable}")
        outputContains("javadoc executable = ${jvm.javadocExecutable}")
    }

    @Unroll
    def "plugin can query information about JDK #version install"() {
        def jvm = AvailableJavaHomes.getJdk(version)
        Assume.assumeTrue(jvm != null)

        taskTypeShowsJavaInstallationDetails()
        buildFile << """
            task show(type: ShowTask) {
                installation = services.get(JavaInstallationRegistry).installationForDirectory(project.layout.projectDirectory.dir("${jvm.javaHome.toURI()}"))
            }
        """

        when:
        run("show")

        then:
        outputContains("install dir = ${jvm.javaHome}")
        outputContains("java version = ${jvm.javaVersion}")
        outputContains("java executable = ${jvm.javaExecutable}")
        outputContains("JDK? = true")
        outputContains("javac executable = ${jvm.javacExecutable}")
        outputContains("javadoc executable = ${jvm.javadocExecutable}")

        where:
        version << [JavaVersion.VERSION_1_5, JavaVersion.VERSION_1_6, JavaVersion.VERSION_1_7]
    }

    @IgnoreIf({ OperatingSystem.current().windows }) // FIXME: Test fails on Windows for unknown reason
    def "plugin can query information about a standalone JRE install alongside a JDK"() {
        def jvm = AvailableJavaHomes.availableJvms.find { it.standaloneJre != null }
        Assume.assumeTrue(jvm != null)
        def jre = jvm.standaloneJre

        taskTypeShowsJavaInstallationDetails()
        buildFile << """
            task show(type: ShowTask) {
                installation = services.get(JavaInstallationRegistry).installationForDirectory(project.layout.projectDirectory.dir("${jre.toURI()}"))
            }
        """

        when:
        run("show")

        then:
        outputContains("install dir = ${jvm.javaHome}")
        outputContains("java version = ${jvm.javaVersion}")
        outputContains("java executable = ${jvm.javaExecutable}")
        outputContains("JDK? = true")
        outputContains("javac executable = ${jvm.javacExecutable}")
        outputContains("javadoc executable = ${jvm.javadocExecutable}")
    }

    def "plugin can query information about an JRE install contained within a JDK install"() {
        def jvm = AvailableJavaHomes.availableJvms.find { it.embeddedJre != null }
        Assume.assumeTrue(jvm != null)
        def jre = jvm.embeddedJre

        taskTypeShowsJavaInstallationDetails()
        buildFile << """
            task show(type: ShowTask) {
                installation = services.get(JavaInstallationRegistry).installationForDirectory(project.layout.projectDirectory.dir("${jre.toURI()}"))
            }
        """

        when:
        run("show")

        then:
        outputContains("install dir = ${jvm.javaHome}")
        outputContains("java version = ${jvm.javaVersion}")
        outputContains("java executable = ${jvm.javaExecutable}")
        outputContains("JDK? = true")
        outputContains("javac executable = ${jvm.javacExecutable}")
        outputContains("javadoc executable = ${jvm.javadocExecutable}")
    }

    @IgnoreIf({ AvailableJavaHomes.differentVersion == null })
    @Requires(TestPrecondition.SYMLINKS)
    @ToBeFixedForConfigurationCache(because = "installationForDirectory(dir) not configuration cache ready")
    def "notices changes to Java installation between builds"() {
        def jvm = AvailableJavaHomes.differentVersion

        taskTypeShowsJavaInstallationDetails()
        buildFile << """
            task show(type: ShowTask) {
                installation = services.get(JavaInstallationRegistry).installationForDirectory(project.layout.projectDirectory.dir("install"))
            }
        """

        def javaHome = file("install")
        javaHome.createLink(jvm.javaHome)

        when:
        run("show")

        then:
        outputContains("install dir = ${jvm.javaHome}")
        outputContains("java version = ${jvm.javaVersion}")

        when:
        javaHome.createLink(Jvm.current().javaHome)
        expectDocumentedDeprecationWarning()
        run("show")

        then:
        outputContains("install dir = ${Jvm.current().javaHome}")
        outputContains("java version = ${Jvm.current().javaVersion}")
    }

    @ToBeFixedForConfigurationCache(because = "gradle/configuration-cache#268")
    def "reports unrecognized Java installation"() {
        file("install/bin/java").createFile()

        taskTypeShowsJavaInstallationDetails()
        buildFile << """
            task show(type: ShowTask) {
                installation = services.get(JavaInstallationRegistry).installationForDirectory(project.layout.projectDirectory.dir("install"))
            }
        """

        when:
        fails("show")

        then:
        // TODO - improve the error message for common failures
        failure.assertHasDescription("Execution failed for task ':show'.")
        failure.assertHasCause("Could not determine the details of Java installation in directory ${file("install")}.")
    }

    def pluginShowsCurrentJvm() {
        buildFile << """
            abstract class ShowPlugin implements Plugin<Project> {
                @Inject
                abstract JavaInstallationRegistry getRegistry()

                void apply(Project project) {
                    project.tasks.register("show", ShowTask) {
                        installation = registry.installationForCurrentVirtualMachine
                    }
                }
            }

            apply plugin: ShowPlugin
        """
    }

    def taskTypeShowsJavaInstallationDetails() {
        buildFile << """
            abstract class ShowTask extends DefaultTask {
                @Internal
                abstract Property<JavaInstallation> getInstallation()

                @TaskAction
                def show() {
                    def javaInstallation = installation.get()
                    println("install dir = \${javaInstallation.installationDirectory.asFile}")
                    println("java version = \${javaInstallation.javaVersion}")
                    println("java executable = \${javaInstallation.javaExecutable.asFile}")
                    println("implementation name = \${javaInstallation.implementationName}")
                    println("JDK? = \${javaInstallation.jdk.present}")
                    if (javaInstallation.jdk.present) {
                        println("javac executable = \${javaInstallation.jdk.get().javacExecutable.asFile}")
                        println("javadoc executable = \${javaInstallation.jdk.get().javadocExecutable.asFile}")
                        println("tools classpath = \${javaInstallation.jdk.get().toolsClasspath.files}")
                    }
                }
            }
        """
    }
}
