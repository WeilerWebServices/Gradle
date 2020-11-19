/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.integtests.fixtures.versions

/**
 * Kotlin Gradle Plugin Versions.
 */
class KotlinGradlePluginVersions {

    // https://search.maven.org/search?q=g:org.jetbrains.kotlin%20AND%20a:kotlin-project&core=gav
    private static final List<String> LATEST_VERSIONS = ['1.3.21', '1.3.31', '1.3.41', '1.3.50', '1.3.61', '1.3.72', '1.4.0', '1.4.10', '1.4.20-RC']

    List<String> getLatests() {
        return LATEST_VERSIONS
    }
}
