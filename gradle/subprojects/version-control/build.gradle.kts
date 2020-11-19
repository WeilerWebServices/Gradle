/*
 * Copyright 2012 the original author or authors.
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
    implementation(project(":messaging"))
    implementation(project(":logging"))
    implementation(project(":files"))
    implementation(project(":file-collections"))
    implementation(project(":persistent-cache"))
    implementation(project(":core-api"))
    implementation(project(":core"))
    implementation(project(":resources"))
    implementation(project(":dependency-management"))

    implementation(libs.guava)
    implementation(libs.inject)
    implementation(libs.jgit)
    implementation(libs.commonsHttpclient)
    implementation(libs.jsch)

    testImplementation(project(":native"))
    testImplementation(project(":snapshots"))
    testImplementation(project(":process-services"))
    testImplementation(testFixtures(project(":core")))

    testFixturesImplementation(project(":base-services"))
    testFixturesImplementation(project(":internal-integ-testing"))

    testFixturesImplementation(libs.jgit)
    testFixturesImplementation(libs.commonsIo)
    testFixturesImplementation(libs.commonsHttpclient)
    testFixturesImplementation(libs.jsch)
    testFixturesImplementation(libs.guava)

    integTestImplementation(project(":launcher"))
    integTestDistributionRuntimeOnly(project(":distributions-basics"))
}
