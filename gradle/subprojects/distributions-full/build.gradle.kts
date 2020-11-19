import gradlebuild.run.tasks.RunEmbeddedGradle

plugins {
    id("gradlebuild.distribution.packaging")
    id("gradlebuild.verify-build-environment")
    id("gradlebuild.install")
}

dependencies {
    coreRuntimeOnly(platform(project(":core-platform")))

    pluginsRuntimeOnly(platform(project(":distributions-publishing")))
    pluginsRuntimeOnly(platform(project(":distributions-jvm")))
    pluginsRuntimeOnly(platform(project(":distributions-native")))

    pluginsRuntimeOnly(project(":plugin-development"))
    pluginsRuntimeOnly(project(":build-init"))
    pluginsRuntimeOnly(project(":build-profile"))
    pluginsRuntimeOnly(project(":antlr"))
    pluginsRuntimeOnly(project(":enterprise"))

    // The following are scheduled to be removed from the distribution completely in Gradle 7.0
    pluginsRuntimeOnly(project(":javascript"))
    pluginsRuntimeOnly(project(":platform-play"))
    pluginsRuntimeOnly(project(":ide-play"))
}

tasks.register<RunEmbeddedGradle>("runDevGradle") {
    group = "verification"
    description = "Runs an embedded Gradle using the partial distribution for ${project.path}."
    gradleClasspath.from(configurations.runtimeClasspath.get(), tasks.runtimeApiInfoJar)
}

// This is required for the separate promotion build and should be adjusted there in the future
tasks.register<Copy>("copyDistributionsToRootBuild") {
    dependsOn("buildDists")
    from(layout.buildDirectory.dir("distributions"))
    into(rootProject.layout.buildDirectory.dir("distributions"))
}
