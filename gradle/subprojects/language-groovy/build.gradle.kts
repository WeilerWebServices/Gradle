plugins {
    id("gradlebuild.distribution.api-java")
}

dependencies {
    implementation(project(":base-services"))
    implementation(project(":logging"))
    implementation(project(":process-services"))
    implementation(project(":worker-processes"))
    implementation(project(":file-collections"))
    implementation(project(":core-api"))
    implementation(project(":model-core"))
    implementation(project(":core"))
    implementation(project(":jvm-services"))
    implementation(project(":workers"))
    implementation(project(":platform-base"))
    implementation(project(":platform-jvm"))
    implementation(project(":language-jvm"))
    implementation(project(":language-java"))
    implementation(project(":files"))

    implementation(libs.slf4jApi)
    implementation(libs.groovy)
    implementation(libs.guava)
    implementation(libs.asm)
    implementation(libs.inject)

    testImplementation(project(":base-services-groovy"))
    testImplementation(project(":internal-testing"))
    testImplementation(project(":resources"))
    testImplementation(testFixtures(project(":core")))

    testFixturesApi(testFixtures(project(":language-jvm")))
    testFixturesImplementation(project(":core"))
    testFixturesImplementation(project(":base-services"))

    integTestImplementation(libs.commonsLang)

    testRuntimeOnly(project(":distributions-core")) {
        because("Tests instantiate DefaultClassLoaderRegistry which requires a 'gradle-plugins.properties' through DefaultPluginModuleRegistry")
    }
    integTestDistributionRuntimeOnly(project(":distributions-core"))
}

classycle {
    excludePatterns.set(listOf(
        "org/gradle/api/internal/tasks/compile/**",
        "org/gradle/api/tasks/javadoc/**"
    ))
}
