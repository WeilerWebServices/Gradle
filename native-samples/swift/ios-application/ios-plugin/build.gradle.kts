/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    groovy
    `java-gradle-plugin`
}

group = "org.gradle.samples"
version = "1.0"

dependencies {
    implementation("commons-io:commons-io:2.6")
    implementation("com.googlecode.plist:dd-plist:1.20")
}

repositories {
    mavenCentral()
}

gradlePlugin {
    (plugins) {
        register("ios-application") {
            id = "org.gradle.samples.ios-application"
            implementationClass = "org.gradle.samples.plugins.IOSApplicationPlugin"
        }
    }
}