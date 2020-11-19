plugins {
    application
    kotlin("jvm") version "1.3.70"
    kotlin("kapt") version "1.3.70"
}

application {
    mainClassName = "samples.HelloAutoValueKt"
}

repositories {
    jcenter()
}

dependencies {
    compileOnly("com.google.auto.value:auto-value-annotations:1.7")
    kapt("com.google.auto.value:auto-value:1.7")

    compile(kotlin("stdlib"))

    testCompile("junit:junit:4.12")
}

kapt {
    correctErrorTypes = true

    javacOptions {
        option("SomeJavacOption", "OptionValue")
    }

    arguments {
        arg("SomeKaptArgument", "ArgumentValue")
    }
}
