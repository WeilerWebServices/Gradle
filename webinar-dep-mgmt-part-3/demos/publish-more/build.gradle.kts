plugins {
    `java-library`
    `maven-publish`
    `java-test-fixtures`
}

group = "org.test"
version = "0.0.3"

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    jcenter()
}

dependencies {
    api("org.apache.commons:commons-math3:3.6.1")

    implementation("com.google.guava:guava:28.0-jre")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")

    testFixturesApi("org.assertj:assertj-core:3.13.2")
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("main") {
            from(components["java"])
            suppressAllPomMetadataWarnings()
        }
    }
    repositories {
        maven {
            name = "local"
            url = uri("$buildDir/repo")
        }
    }
}
