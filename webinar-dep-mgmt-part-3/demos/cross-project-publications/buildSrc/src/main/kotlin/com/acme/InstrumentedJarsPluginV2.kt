/*
 * Copyright 2019 the original author or authors.
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

package com.acme

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.register

class InstrumentedJarsPluginV2 : Plugin<Project> {

    override fun apply(project: Project) = project.run {
        val outgoingConfiguration = createOutgoingConfiguration()
        attachArtifact(outgoingConfiguration)
    }

    private fun Project.attachArtifact(outgoingConfiguration: Configuration) {
        val instrumentedJar = tasks.register<Jar>("instrumentedJar") {
            archiveClassifier.set("instrumented")
        }
        outgoingConfiguration.outgoing.artifact(instrumentedJar)
    }

    // tag::create_outgoing_configuration[]
    private fun Project.createOutgoingConfiguration(): Configuration {
        val instrumentedJars by configurations.creating {
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes {
                attribute(Category.CATEGORY_ATTRIBUTE, namedAttribute(Category.LIBRARY))
                attribute(Usage.USAGE_ATTRIBUTE, namedAttribute(Usage.JAVA_RUNTIME))
                attribute(Bundling.BUNDLING_ATTRIBUTE, namedAttribute(Bundling.EXTERNAL))
                attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, JavaVersion.current().majorVersion.toInt())
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, namedAttribute("instrumented-jar"))
            }
        }
        return instrumentedJars
    }
    // end::create_outgoing_configuration[]

}
