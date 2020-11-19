dependencies {
    implementation(project(":basics"))
    implementation(project(":module-identity"))

    implementation("com.github.javaparser:javaparser-core")
    implementation("com.google.guava:guava")
    implementation("com.uwyn:jhighlight") {
        exclude(module = "servlet-api")
    }
    implementation("com.vladsch.flexmark:flexmark-all")
    implementation("commons-lang:commons-lang")
    implementation("org.asciidoctor:asciidoctor-gradle-plugin")
    implementation("org.asciidoctor:asciidoctorj")
    implementation("org.asciidoctor:asciidoctorj-pdf")
}

gradlePlugin {
    plugins {
        register("gradleDocumentation") {
            id = "gradlebuild.documentation"
            implementationClass = "gradlebuild.docs.GradleBuildDocumentationPlugin"
        }
    }
}
