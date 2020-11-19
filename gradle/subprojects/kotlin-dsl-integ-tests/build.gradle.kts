/*
 * Copyright 2018 the original author or authors.
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

import gradlebuild.cleanup.WhenNotEmpty

plugins {
    id("gradlebuild.internal.kotlin")
}

description = "Kotlin DSL Integration Tests"

dependencies {
    testImplementation(testFixtures(project(":kotlin-dsl")))

    integTestImplementation(project(":base-services"))
    integTestImplementation(project(":core-api"))
    integTestImplementation(project(":core"))
    integTestImplementation(project(":internal-testing"))
    integTestImplementation("com.squareup.okhttp3:mockwebserver:3.9.1")

    integTestDistributionRuntimeOnly(project(":distributions-full"))

    integTestLocalRepository(project(":kotlin-dsl-plugins"))
}

val pluginBundles = listOf(
    ":kotlin-dsl-plugins"
)

pluginBundles.forEach {
    evaluationDependsOn(it)
}

tasks {
    val writeFuturePluginVersions by registering {

        group = "build"
        description = "Merges all future plugin bundle versions so they can all be tested at once"

        val futurePluginVersionsTasks =
            pluginBundles.map {
                project(it).tasks["writeFuturePluginVersions"] as WriteProperties
            }

        dependsOn(futurePluginVersionsTasks)
        inputs.files(futurePluginVersionsTasks.map { it.outputFile })
        outputs.file(layout.buildDirectory.file("generated-resources/future-plugin-versions/future-plugin-versions.properties"))

        doLast {
            outputs.files.singleFile.bufferedWriter().use { writer ->
                inputs.files.forEach { input ->
                    writer.appendln(input.readText())
                }
            }
        }
    }
    sourceSets.integTest.get().output.dir(
        writeFuturePluginVersions.map { it.outputs.files.singleFile.parentFile }
    )
}

testFilesCleanup {
    policy.set(WhenNotEmpty.REPORT)
}
