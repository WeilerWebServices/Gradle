plugins {
    `java-library`
}

repositories {
    jcenter()
}

// tag::consumer[]
dependencies {
    // This project requires the main producer component
    implementation(project(":producer"))

    // But we also want to use its MySQL support
    runtimeOnly(project(":producer")) {
        capabilities {
            requireCapability("org.gradle.demo:producer-mysql-support")
        }
    }
}
// end::consumer[]
