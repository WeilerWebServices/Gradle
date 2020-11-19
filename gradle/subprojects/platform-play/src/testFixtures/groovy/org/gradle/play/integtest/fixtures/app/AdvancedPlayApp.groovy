/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.play.integtest.fixtures.app

import org.gradle.integtests.fixtures.SourceFile
import org.gradle.play.integtest.fixtures.PlayApp
import org.gradle.util.VersionNumber

import static org.gradle.play.integtest.fixtures.Repositories.*

class AdvancedPlayApp extends PlayApp {
    AdvancedPlayApp(VersionNumber version) {
        super(version)
    }
    @Override
    SourceFile getGradleBuild() {
        def gradleBuild = super.getGradleBuild()
        def gradleBuildWithRepositories = gradleBuild.content.concat """
            allprojects {
                ${GRADLE_JS_REPOSITORY}
            }
        """
        return new SourceFile(gradleBuild.path, gradleBuild.name, gradleBuildWithRepositories)
    }

    @Override
    List<SourceFile> getViewSources() {
        return super.getViewSources() + sourceFiles("templates")
    }
}
