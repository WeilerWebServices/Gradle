/*
 * Copyright 2014 the original author or authors.
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
plugins {
    id("gradlebuild.distribution.api-java")
}

dependencies {
    implementation(project(":base-services"))
    implementation(project(":native"))
    implementation(project(":logging"))
    implementation(project(":process-services"))
    implementation(project(":core-api"))
    implementation(project(":model-core"))
    implementation(project(":core"))
    implementation(project(":diagnostics"))
    implementation(project(":reporting"))
    implementation(project(":platform-base"))
    implementation(project(":platform-native"))
    implementation(project(":language-native"))
    implementation(project(":testing-base"))

    implementation(libs.groovy)
    implementation(libs.slf4jApi)
    implementation(libs.guava)
    implementation(libs.commonsLang)
    implementation(libs.commonsIo)
    implementation(libs.inject)

    testImplementation(project(":file-collections"))
    testImplementation(testFixtures(project(":core")))
    testImplementation(testFixtures(project(":platform-native")))
    testImplementation(testFixtures(project(":diagnostics")))
    testImplementation(testFixtures(project(":platform-base")))
    testImplementation(testFixtures(project(":testing-base")))
    testImplementation(testFixtures(project(":language-native")))
    testImplementation(testFixtures(project(":ide")))

    testRuntimeOnly(project(":distributions-core")) {
        because("ProjectBuilder tests load services from a Gradle distribution.")
    }
    integTestDistributionRuntimeOnly(project(":distributions-native"))
}
