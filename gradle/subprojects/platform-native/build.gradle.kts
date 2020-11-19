/*
 * Copyright 2011 the original author or authors.
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
import gradlebuild.integrationtests.integrationTestUsesSampleDir

plugins {
    id("gradlebuild.distribution.api-java")
}

dependencies {
    implementation(project(":base-services"))
    implementation(project(":logging"))
    implementation(project(":native"))
    implementation(project(":process-services"))
    implementation(project(":file-collections"))
    implementation(project(":core-api"))
    implementation(project(":model-core"))
    implementation(project(":core"))
    implementation(project(":workers"))
    implementation(project(":platform-base"))
    implementation(project(":diagnostics"))

    implementation(libs.nativePlatform)
    implementation(libs.groovy)
    implementation(libs.slf4jApi)
    implementation(libs.guava)
    implementation(libs.commonsLang)
    implementation(libs.commonsIo)
    implementation(libs.snakeyaml)
    implementation(libs.gson)
    implementation(libs.inject)

    testFixturesApi(project(":resources"))
    testFixturesApi(testFixtures(project(":ide")))
    testFixturesImplementation(testFixtures(project(":core")))
    testFixturesImplementation(project(":internal-integ-testing"))
    testFixturesImplementation(project(":native"))
    testFixturesImplementation(project(":platform-base"))
    testFixturesImplementation(project(":file-collections"))
    testFixturesImplementation(project(":process-services"))
    testFixturesImplementation(project(":snapshots"))
    testFixturesImplementation(libs.guava)
    testFixturesImplementation(libs.nativePlatform)
    testFixturesImplementation(libs.commonsLang)
    testFixturesImplementation(libs.commonsIo)

    testImplementation(testFixtures(project(":core")))
    testImplementation(testFixtures(project(":messaging")))
    testImplementation(testFixtures(project(":platform-base")))
    testImplementation(testFixtures(project(":model-core")))
    testImplementation(testFixtures(project(":diagnostics")))
    testImplementation(testFixtures(project(":base-services")))
    testImplementation(testFixtures(project(":snapshots")))

    testRuntimeOnly(project(":distributions-core")) {
        because("ProjectBuilder tests load services from a Gradle distribution.")
    }
    integTestDistributionRuntimeOnly(project(":distributions-native")) {
        because("Required 'ideNative' to test visual studio project file generation for generated sources")
    }
}

classycle {
    excludePatterns.set(listOf(
        "org/gradle/nativeplatform/plugins/**",
        "org/gradle/nativeplatform/tasks/**",
        "org/gradle/nativeplatform/internal/resolve/**",
        "org/gradle/nativeplatform/toolchain/internal/**"
    ))
}

integrationTestUsesSampleDir("subprojects/platform-native/src/main")
