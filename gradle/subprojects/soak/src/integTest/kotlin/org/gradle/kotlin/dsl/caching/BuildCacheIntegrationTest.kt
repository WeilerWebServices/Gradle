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

package org.gradle.kotlin.dsl.caching

import org.gradle.kotlin.dsl.caching.fixtures.CachedScript
import org.gradle.kotlin.dsl.caching.fixtures.cachedBuildFile
import org.gradle.kotlin.dsl.caching.fixtures.cachedSettingsFile
import org.gradle.kotlin.dsl.caching.fixtures.compilationCache
import org.gradle.kotlin.dsl.fixtures.normalisedPath

import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Ignore

import org.junit.Test

import java.io.File


class BuildCacheIntegrationTest : AbstractScriptCachingIntegrationTest() {

    @Test
    fun `can publish build scan`() {

        val buildCacheDir = existing("build-cache")

        withLocalBuildCacheSettings(buildCacheDir)

        val settingsFile = existing("settings.gradle.kts")
        settingsFile.writeText(
            """
            plugins {
                `gradle-enterprise`
            }

            gradleEnterprise.buildScan {
                termsOfServiceUrl = "https://gradle.com/terms-of-service"
                termsOfServiceAgree = "yes"
            }
            """ + settingsFile.readText()
        )

        build("--scan", "--build-cache", "-Dscan.dump").apply {
            assertThat(output, containsString("Build scan written to"))
        }
    }

    @Test
    @Ignore("https://github.com/gradle/gradle-private/issues/3233")
    fun `build cache integration can be disabled via system property`() {

        val buildCacheDir = existing("build-cache")

        val expectedOutput = "***42***"

        fun cloneProject(): Pair<CachedScript.WholeFile, CachedScript.WholeFile> {

            val settingsFile =
                withLocalBuildCacheSettings(buildCacheDir)

            val buildFile = withBuildScript(
                """
                    plugins {
                        java // force the generation of accessors
                    }

                    println("$expectedOutput")
                """
            )

            return cachedSettingsFile(settingsFile, hasBody = true) to cachedBuildFile(buildFile, hasBody = true)
        }

        withProjectRoot(newDir("clone-a")) {

            val (settingsFile, buildFile) = cloneProject()

            // Cache miss with a fresh Gradle home, script cache will be pushed to build cache
            executer.withGradleUserHomeDir(newDir("guh-1"))
            buildForCacheInspection("--build-cache").apply {

                compilationCache {
                    misses(settingsFile)
                    misses(buildFile)
                }

                assertThat(output, containsString(expectedOutput))
            }
        }

        withProjectRoot(newDir("clone-b")) {

            val (settingsFile, buildFile) = cloneProject()

            // Cache hit from build cache
            executer.withGradleUserHomeDir(newDir("guh-2"))
            buildForCacheInspection("--build-cache").apply {

                compilationCache {
                    misses(settingsFile)
                    hits(buildFile)
                }

                assertThat(output, containsString(expectedOutput))
            }

            // Cache miss without build cache integration (disabled via system property)
            executer.withGradleUserHomeDir(
                newDir("guh-3").apply {
                    resolve("gradle.properties").writeText(
                        "systemProp.org.gradle.kotlin.dsl.caching.buildcache=false"
                    )
                }
            )
            buildForCacheInspection("--build-cache").apply {

                compilationCache {
                    misses(settingsFile)
                    misses(buildFile)
                }

                assertThat(output, containsString(expectedOutput))
            }
        }
    }

    private
    fun withLocalBuildCacheSettings(buildCacheDir: File): File =
        withSettings(
            """
            buildCache {
                local {
                    directory = file("${buildCacheDir.normalisedPath}")
                    isEnabled = true
                    isPush = true
                }
            }
            """
        )
}
