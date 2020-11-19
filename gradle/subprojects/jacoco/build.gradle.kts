/*
 * Copyright 2013 the original author or authors.
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
    implementation(project(":logging"))
    implementation(project(":process-services"))
    implementation(project(":core-api"))
    implementation(project(":model-core"))
    implementation(project(":core"))
    implementation(project(":platform-base"))
    implementation(project(":testing-base"))
    implementation(project(":testing-jvm"))
    implementation(project(":plugins"))
    implementation(project(":reporting"))
    implementation(project(":file-collections"))

    implementation(libs.groovy)
    implementation(libs.slf4jApi)
    implementation(libs.guava)
    implementation(libs.commonsLang)
    implementation(libs.inject)

    testFixturesImplementation(project(":base-services"))
    testFixturesImplementation(project(":core-api"))
    testFixturesImplementation(project(":core"))
    testFixturesImplementation(project(":internal-integ-testing"))
    testFixturesImplementation(libs.jsoup)

    testImplementation(project(":internal-testing"))
    testImplementation(project(":resources"))
    testImplementation(project(":internal-integ-testing"))
    testImplementation(project(":language-java"))
    testImplementation(testFixtures(project(":core")))

    testRuntimeOnly(project(":distributions-core")) {
        because("ProjectBuilder tests load services from a Gradle distribution.")
    }
    integTestDistributionRuntimeOnly(project(":distributions-jvm"))
}

strictCompile {
    ignoreRawTypes()
}

classycle {
    excludePatterns.set(listOf(
        "org/gradle/internal/jacoco/*",
        "org/gradle/testing/jacoco/plugins/*"))
}
