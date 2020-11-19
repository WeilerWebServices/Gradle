/*
 * Copyright 2017 the original author or authors.
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
    implementation(project(":native"))
    implementation(project(":files"))
    implementation(project(":resources"))
    implementation(project(":logging"))

    implementation(libs.slf4jApi)
    implementation(libs.guava)
    implementation(libs.commonsIo)
    implementation(libs.commonsLang)

    testImplementation(project(":core-api"))
    testImplementation(testFixtures(project(":core")))

    testRuntimeOnly(project(":distributions-core")) {
        because("DefaultPersistentDirectoryCacheTest instantiates DefaultClassLoaderRegistry which requires a 'gradle-plugins.properties' through DefaultPluginModuleRegistry")
    }
    integTestDistributionRuntimeOnly(project(":distributions-core"))
}
