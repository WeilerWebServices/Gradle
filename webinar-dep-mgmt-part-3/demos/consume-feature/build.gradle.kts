plugins {
    `java-library`
}

repositories {
    maven {
        name = "local"
        url = uri("$projectDir/../publish-feature/build/repo")
    }
    jcenter()
}

dependencies {
    implementation("org.test:publish-feature:0.0.1")

    implementation("org.test:publish-feature:0.0.1") {
        capabilities {
            requireCapability("org.test:publish-feature-optional-feature")
        }
    }

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}
