plugins {
    `java-library`
    `maven-publish`
}

group = "org.test"
version = "0.0.1"

repositories {
    jcenter()
}

java {
    registerFeature("optionalFeature") {
        usingSourceSet(sourceSets["main"])
    }
}

dependencies {
    api("org.apache.commons:commons-math3:3.6.1")

    implementation("com.google.guava:guava:28.0-jre")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")

    "optionalFeatureImplementation"("org.apache.commons:commons-lang3:3.9")
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