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
import gradlebuild.basics.BuildEnvironment
import gradlebuild.basics.accessors.groovy
import gradlebuild.integrationtests.addDependenciesAndConfigurations
import gradlebuild.integrationtests.tasks.SmokeTest
import gradlebuild.performance.generator.tasks.RemoteProject

plugins {
    id("gradlebuild.internal.java")
}

val smokeTestSourceSet = sourceSets.create("smokeTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

addDependenciesAndConfigurations("smoke")

val smokeTestImplementation: Configuration by configurations.getting
val smokeTestDistributionRuntimeOnly: Configuration by configurations.getting

dependencies {
    smokeTestImplementation(project(":base-services"))
    smokeTestImplementation(project(":core-api"))
    smokeTestImplementation(project(":test-kit"))
    smokeTestImplementation(project(":launcher"))
    smokeTestImplementation(project(":persistent-cache"))
    smokeTestImplementation(project(":jvm-services"))
    smokeTestImplementation(project(":build-option"))
    smokeTestImplementation(libs.commonsIo)
    smokeTestImplementation(libs.jgit)
    smokeTestImplementation(libs.gradleProfiler) {
        because("Using build mutators to change a Java file")
    }
    smokeTestImplementation(libs.spock)

    smokeTestImplementation(testFixtures(project(":core")))
    smokeTestImplementation(testFixtures(project(":version-control")))

    smokeTestDistributionRuntimeOnly(project(":distributions-full"))
}

tasks {

    /**
     * Santa Tracker git URI.
     *
     * Note that you can change it to `file:///path/to/your/santa-tracker-clone/.git`
     * if you need to iterate quickly on changes to Santa Tracker.
     */
    val santaGitUri = "https://github.com/gradle/santa-tracker-android.git"

    val santaTrackerKotlin by registering(RemoteProject::class) {
        remoteUri.set(santaGitUri)
        // Pinned from branch agp-3.6.0
        ref.set("3122343674246eaa56a5dd6365ea137edb021780")
    }

    val santaTrackerJava by registering(RemoteProject::class) {
        remoteUri.set(santaGitUri)
        // Pinned from branch agp-3.6.0-java
        ref.set("d8543e51ac5a4803a8ac57f0639229736f11e0a8")
    }

    val gradleBuildCurrent by registering(RemoteProject::class) {
        remoteUri.set(rootDir.absolutePath)
        ref.set(moduleIdentity.gradleBuildCommitId)
    }

    val santaProjects = arrayOf(santaTrackerKotlin, santaTrackerJava)
    val remoteProjects = santaProjects + gradleBuildCurrent

    if (BuildEnvironment.isCiServer) {
        remoteProjects.forEach { remoteProject ->
            remoteProject {
                outputs.upToDateWhen { false }
            }
        }
    }

    register<Delete>("cleanRemoteProjects") {
        remoteProjects.forEach { remoteProject ->
            delete(remoteProject.map { it.outputDirectory })
        }
    }

    fun SmokeTest.configureForSmokeTest(vararg remoteProjects: TaskProvider<RemoteProject>) {
        group = "Verification"
        testClassesDirs = smokeTestSourceSet.output.classesDirs
        classpath = smokeTestSourceSet.runtimeClasspath
        maxParallelForks = 1 // those tests are pretty expensive, we shouldn"t execute them concurrently
        inputs.property("androidHomeIsSet", System.getenv("ANDROID_HOME") != null)
        inputs.files(remoteProjects.map { it.map { it.outputDirectory } })
            .withPropertyName("remoteProjectsSource")
            .withPathSensitivity(PathSensitivity.RELATIVE)
    }

    val gradleBuildCategory = "org.gradle.smoketests.GradleBuild"

    register<SmokeTest>("smokeTest") {
        description = "Runs Smoke tests"
        configureForSmokeTest(*santaProjects)
        useJUnit {
            excludeCategories(gradleBuildCategory)
        }
    }

    register<SmokeTest>("configCacheSmokeTest") {
        description = "Runs Smoke tests with the configuration cache"
        configureForSmokeTest(*santaProjects)
        systemProperty("org.gradle.integtest.executer", "configCache")
        useJUnit {
            excludeCategories(gradleBuildCategory)
        }
    }

    register<SmokeTest>("gradleBuildSmokeTest") {
        description = "Runs Smoke tests against the Gradle build"
        configureForSmokeTest(gradleBuildCurrent)
        useJUnit {
            includeCategories(gradleBuildCategory)
        }
    }
}

plugins.withType<IdeaPlugin>().configureEach {
    val smokeTestCompileClasspath: Configuration by configurations.getting
    val smokeTestRuntimeClasspath: Configuration by configurations.getting
    model.module {
        testSourceDirs = testSourceDirs + smokeTestSourceSet.groovy.srcDirs
        testResourceDirs = testResourceDirs + smokeTestSourceSet.resources.srcDirs
        scopes["TEST"]!!["plus"]!!.add(smokeTestCompileClasspath)
        scopes["TEST"]!!["plus"]!!.add(smokeTestRuntimeClasspath)
    }
}
