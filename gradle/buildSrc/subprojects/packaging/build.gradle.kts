dependencies {
    implementation(project(":documentation")) {
        // TODO turn this around: move corresponding code to this project and let docs depend on it
        because("API metadata generation is part of the DSL guide")
    }
    implementation(project(":basics"))
    implementation(project(":module-identity"))
    implementation(project(":jvm"))

    implementation("com.google.code.gson:gson")

    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
