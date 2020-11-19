import org.gradle.api.internal.FeaturePreviews

/*
 * Copyright 2010 the original author or authors.
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

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
    }
}

plugins {
    id("com.gradle.enterprise").version("3.5")
    id("com.gradle.enterprise.gradle-enterprise-conventions-plugin").version("0.7.1")
}

apply(from = "gradle/shared-with-buildSrc/mirrors.settings.gradle.kts")

// If you include a new subproject here, you will need to execute the
// ./gradlew generateSubprojectsInfo
// task to update metadata about the build for CI

include("distributions-dependencies") // platform for dependency versions
include("core-platform")              // platform for Gradle distribution core

// Gradle Distributions - for testing and for publishing a full distribution
include("distributions-core")
include("distributions-basics")
include("distributions-publishing")
include("distributions-jvm")
include("distributions-native")
include("distributions-full")

// Gradle implementation projects
include("configuration-cache")
include("api-metadata")
include("base-services")
include("base-services-groovy")
include("logging")
include("process-services")
include("jvm-services")
include("core")
include("dependency-management")
include("wrapper")
include("cli")
include("launcher")
include("bootstrap")
include("messaging")
include("resources")
include("resources-http")
include("resources-gcs")
include("resources-s3")
include("resources-sftp")
include("plugins")
include("scala")
include("ide")
include("ide-native")
include("ide-play")
include("maven")
include("code-quality")
include("antlr")
include("tooling-api")
include("build-events")
include("tooling-api-builders")
include("signing")
include("ear")
include("native")
include("javascript")
include("reporting")
include("diagnostics")
include("publish")
include("ivy")
include("jacoco")
include("build-init")
include("build-option")
include("platform-base")
include("platform-native")
include("platform-jvm")
include("language-jvm")
include("language-java")
include("java-compiler-plugin")
include("language-groovy")
include("language-native")
include("tooling-native")
include("language-scala")
include("plugin-use")
include("plugin-development")
include("model-core")
include("model-groovy")
include("build-cache-http")
include("testing-base")
include("testing-native")
include("testing-jvm")
include("testing-junit-platform")
include("platform-play")
include("test-kit")
include("installation-beacon")
include("composite-builds")
include("workers")
include("persistent-cache")
include("build-cache-base")
include("build-cache")
include("core-api")
include("version-control")
include("file-collections")
include("files")
include("hashing")
include("snapshots")
include("file-watching")
include("build-cache-packaging")
include("execution")
include("build-profile")
include("kotlin-compiler-embeddable")
include("kotlin-dsl")
include("kotlin-dsl-provider-plugins")
include("kotlin-dsl-tooling-models")
include("kotlin-dsl-tooling-builders")
include("worker-processes")
include("base-annotations")
include("security")
include("normalization-java")
include("enterprise")
include("build-operations")

// Plugin portal projects
include("kotlin-dsl-plugins")

// Internal utility and verification projects
include("docs")
include("samples")
include("architecture-test")
include("internal-testing")
include("internal-integ-testing")
include("internal-performance-testing")
include("internal-android-performance-testing")
include("internal-build-reports")
include("integ-test")
include("kotlin-dsl-integ-tests")
include("distributions-integ-tests")
include("soak")
include("smoke-test")
include("performance")
include("build-scan-performance")
include("configuration-cache-report")

rootProject.name = "gradle"

for (project in rootProject.children) {
    project.projectDir = file("subprojects/${project.name}")
}

FeaturePreviews.Feature.values().forEach { feature ->
    if (feature.isActive) {
        enableFeaturePreview(feature.name)
    }
}
