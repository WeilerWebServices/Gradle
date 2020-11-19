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

package org.gradle.api.plugins

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.test.fixtures.AbstractProjectBuilderSpec
import org.gradle.util.TestUtil
import spock.lang.Unroll

import static org.gradle.util.WrapUtil.toSet

class JavaLibraryPluginTest extends AbstractProjectBuilderSpec {
    def "applies Java plugin"() {
        when:
        project.pluginManager.apply(JavaLibraryPlugin)

        then:
        project.plugins.findPlugin(JavaPlugin)
    }

    def "adds configurations to the project"() {
        given:
        project.pluginManager.apply(JavaLibraryPlugin)

        when:
        def compile = project.configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME)

        then:
        compile.extendsFrom == [] as Set
        !compile.visible
        compile.transitive

        when:
        def api = project.configurations.getByName(JavaPlugin.API_CONFIGURATION_NAME)

        then:
        !api.visible
        api.extendsFrom == [compile] as Set
        !api.canBeConsumed
        !api.canBeResolved

        when:
        def implementation = project.configurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)

        then:
        !implementation.visible
        implementation.extendsFrom == [api, compile] as Set
        !implementation.canBeConsumed
        !implementation.canBeResolved

        when:
        def runtime = project.configurations.getByName(JavaPlugin.RUNTIME_CONFIGURATION_NAME)

        then:
        runtime.extendsFrom == toSet(compile)
        !runtime.visible
        runtime.transitive

        when:
        def runtimeOnly = project.configurations.getByName(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME)

        then:
        runtimeOnly.transitive
        !runtimeOnly.visible
        !runtimeOnly.canBeConsumed
        !runtimeOnly.canBeResolved
        runtimeOnly.extendsFrom == [] as Set

        when:
        def runtimeElements = project.configurations.getByName(JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME)

        then:
        runtimeElements.transitive
        !runtimeElements.visible
        runtimeElements.canBeConsumed
        !runtimeElements.canBeResolved
        runtimeElements.extendsFrom == [implementation, runtimeOnly, runtime] as Set

        when:
        def runtimeClasspath = project.configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)

        then:
        runtimeClasspath.transitive
        !runtimeClasspath.visible
        !runtimeClasspath.canBeConsumed
        runtimeClasspath.canBeResolved
        runtimeClasspath.extendsFrom == [runtimeOnly, runtime, implementation] as Set

        when:
        def compileOnlyApi = project.configurations.getByName(JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME)

        then:
        compileOnlyApi.extendsFrom == [] as Set
        !compileOnlyApi.visible
        compileOnlyApi.transitive
        !compileOnlyApi.canBeConsumed
        !compileOnlyApi.canBeResolved

        when:
        def compileOnly = project.configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME)

        then:
        compileOnly.extendsFrom == [compileOnlyApi] as Set
        !compileOnly.visible
        compileOnly.transitive

        when:
        def compileClasspath = project.configurations.getByName(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)

        then:
        compileClasspath.extendsFrom == toSet(compileOnly, implementation)
        !compileClasspath.visible
        compileClasspath.transitive

        when:
        def apiElements = project.configurations.getByName(JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME)

        then:
        !apiElements.visible
        apiElements.extendsFrom == [api, runtime, compileOnlyApi] as Set
        apiElements.canBeConsumed
        !apiElements.canBeResolved

        when:
        def testCompile = project.configurations.getByName(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME)

        then:
        testCompile.extendsFrom == toSet(compile)
        !testCompile.visible
        testCompile.transitive

        when:
        def testImplementation = project.configurations.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)

        then:
        testImplementation.extendsFrom == toSet(testCompile, implementation)
        !testImplementation.visible
        !testImplementation.canBeConsumed
        !testImplementation.canBeResolved

        when:
        def testRuntime = project.configurations.getByName(JavaPlugin.TEST_RUNTIME_CONFIGURATION_NAME)

        then:
        testRuntime.extendsFrom == toSet(runtime, testCompile)
        !testRuntime.visible
        testRuntime.transitive

        when:
        def testRuntimeOnly = project.configurations.getByName(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME)

        then:
        testRuntimeOnly.transitive
        !testRuntimeOnly.visible
        !testRuntimeOnly.canBeConsumed
        !testRuntimeOnly.canBeResolved
        testRuntimeOnly.extendsFrom == [runtimeOnly] as Set

        when:
        def testCompileOnly = project.configurations.getByName(JavaPlugin.TEST_COMPILE_ONLY_CONFIGURATION_NAME)

        then:
        testCompileOnly.extendsFrom == [] as Set
        !testCompileOnly.visible
        testCompileOnly.transitive

        when:
        def testCompileClasspath = project.configurations.getByName(JavaPlugin.TEST_COMPILE_CLASSPATH_CONFIGURATION_NAME)

        then:
        testCompileClasspath.extendsFrom == toSet(testCompileOnly, testImplementation)
        !testCompileClasspath.visible
        testCompileClasspath.transitive

        when:
        def defaultConfig = project.configurations.getByName(Dependency.DEFAULT_CONFIGURATION)

        then:
        defaultConfig.extendsFrom == toSet(runtimeElements)
    }

    @Unroll
    def "can declare API and implementation dependencies [compileClasspathPackaging=#compileClasspathPackaging]"() {
        if (compileClasspathPackaging) {
            System.setProperty(JavaBasePlugin.COMPILE_CLASSPATH_PACKAGING_SYSTEM_PROPERTY, "true")
        }

        given:
        def commonProject = TestUtil.createChildProject(project, "common")
        def toolsProject = TestUtil.createChildProject(project, "tools")
        def internalProject = TestUtil.createChildProject(project, "internal")

        project.pluginManager.apply(JavaPlugin)
        commonProject.pluginManager.apply(JavaLibraryPlugin)
        toolsProject.pluginManager.apply(JavaLibraryPlugin)
        internalProject.pluginManager.apply(JavaLibraryPlugin)

        when:
        project.dependencies {
            compile commonProject
        }
        commonProject.dependencies {
            api toolsProject
            implementation internalProject
        }

        def task = project.tasks.compileJava

        then:
        task.taskDependencies.getDependencies(task)*.path as Set == [":common:$producingTask", ":tools:$producingTask"] as Set<String>

        when:
        task = commonProject.tasks.compileJava

        then:
        task.taskDependencies.getDependencies(task)*.path as Set == [":tools:$producingTask", ":internal:$producingTask"] as Set<String>

        cleanup:
        System.setProperty(JavaBasePlugin.COMPILE_CLASSPATH_PACKAGING_SYSTEM_PROPERTY, "")

        where:
        compileClasspathPackaging | producingTask
        true                      | 'jar'
        false                     | 'compileJava'
    }

    def "adds Java library component"() {
        given:
        project.pluginManager.apply(JavaLibraryPlugin)
        project.dependencies.add(JavaPlugin.API_CONFIGURATION_NAME, "org:api1:1.0")
        project.dependencies.constraints.add(JavaPlugin.API_CONFIGURATION_NAME, "org:api2:2.0")
        project.dependencies.add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, "org:impl1:1.0")
        project.dependencies.constraints.add(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, "org:impl2:2.0")

        when:
        def jarTask = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME)
        def javaLibrary = project.components.getByName("java")
        def runtimeUsage = javaLibrary.usages.find { it.name == 'runtimeElements' }
        def apiUsage = javaLibrary.usages.find { it.name == 'apiElements' }

        then:
        runtimeUsage.artifacts.collect {it.file} == [jarTask.archivePath]
        runtimeUsage.dependencies.size() == 2
        runtimeUsage.dependencies == project.configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).allDependencies.withType(ModuleDependency)
        runtimeUsage.dependencyConstraints.size() == 2
        runtimeUsage.dependencyConstraints == project.configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).allDependencyConstraints

        apiUsage.artifacts.collect {it.file} == [jarTask.archivePath]
        apiUsage.dependencies.size() == 1
        apiUsage.dependencies == project.configurations.getByName(JavaPlugin.API_CONFIGURATION_NAME).allDependencies.withType(ModuleDependency)
        apiUsage.dependencyConstraints.size() == 1
        apiUsage.dependencyConstraints == project.configurations.getByName(JavaPlugin.API_CONFIGURATION_NAME).allDependencyConstraints
    }

}
