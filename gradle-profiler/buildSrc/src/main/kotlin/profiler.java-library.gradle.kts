plugins {
    id("java-library")
}

repositories {
    jcenter()
    maven {
        url = uri("https://repo.gradle.org/gradle/repo")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

project.extensions.create<Versions>("versions")

abstract class Versions {
    val toolingApi = "org.gradle:gradle-tooling-api:6.6.1"
    val groovy = "org.codehaus.groovy:groovy:2.4.7"
    val spock = "org.spockframework:spock-core:1.1-groovy-2.4"
}
