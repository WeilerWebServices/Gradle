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
import gradlebuild.basics.googleApisJs

plugins {
    id("gradlebuild.internal.java")
}

val reports by configurations.creating
val flamegraph by configurations.creating
configurations.compileOnly { extendsFrom(flamegraph) }

repositories {
    googleApisJs()
}

dependencies {
    reports("jquery:jquery.min:3.5.1@js")
    reports("flot:flot:0.8.1:min@js")

    api(libs.gradleProfiler) {
        because("Consumers need to instantiate BuildMutators")
    }
    implementation(libs.javaParser) {
        because("The Groovy compiler inspects the dependencies at compile time")
    }

    api(libs.jettyWebApp)

    implementation(project(":base-services"))
    implementation(project(":native"))
    implementation(project(":cli"))
    implementation(project(":logging"))
    implementation(project(":process-services"))
    implementation(project(":core-api"))
    implementation(project(":build-option"))
    implementation(project(":file-collections"))
    implementation(project(":snapshots"))
    implementation(project(":resources"))
    implementation(project(":persistent-cache"))
    implementation(project(":jvm-services"))
    implementation(project(":wrapper"))
    implementation(project(":internal-integ-testing"))

    implementation(libs.junit)
    implementation(libs.spock)
    implementation(libs.commonsIo)
    implementation(libs.commonsLang)
    implementation(libs.guava)
    implementation(libs.groovy)
    implementation(libs.hikariCP)
    implementation(libs.jacksonAnnotations)
    implementation(libs.jacksonCore)
    implementation(libs.jacksonDatabind)
    implementation(libs.slf4jApi)
    implementation(libs.joda)
    implementation(libs.jatl)
    implementation(libs.commonsHttpclient)
    implementation(libs.jsch)
    implementation(libs.commonsMath)
    implementation(libs.jclToSlf4j)
    implementation(libs.flightrecorder)
    implementation(libs.mina)
    implementation(libs.joptSimple)
    implementation(testFixtures(project(":core")))
    implementation(testFixtures(project(":tooling-api")))

    runtimeOnly(libs.mySqlConnector)

    integTestDistributionRuntimeOnly(project(":distributions-core"))
}

val reportResources = tasks.register<Copy>("reportResources") {
    from(reports)
    into(layout.buildDirectory.file("generated-resources/report-resources/org/gradle/reporting"))
}

sourceSets.main {
    output.dir(reportResources.map { it.destinationDir.parentFile.parentFile.parentFile })
}

tasks.jar {
    inputs.files(flamegraph)
        .withPropertyName("flamegraph")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    from(files(provider{ flamegraph.map { zipTree(it) } }))
}
