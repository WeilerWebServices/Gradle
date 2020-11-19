/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.integtests.publish.maven

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.UnsupportedWithConfigurationCache

@UnsupportedWithConfigurationCache(because = "legacy maven plugin")
class MavenEarProjectPublishIntegrationTest extends AbstractIntegrationSpec {

    def setup() {
        // the OLD publish plugins work with the OLD deprecated Java plugin configuration (compile/runtime)
        executer.noDeprecationChecks()
    }

    public void "publishes EAR only for mixed java and WAR and EAR project"() {
        given:
        using m2 //uploadArchives leaks into local ~/.m2
        file("settings.gradle") << "rootProject.name = 'publishTest' "

        and:
        buildFile << """
apply plugin: 'ear'
apply plugin: 'war'
apply plugin: 'maven'

group = 'org.gradle.test'
version = '1.9'

${mavenCentralRepository()}

dependencies {
    compile "commons-collections:commons-collections:3.2.2"
    runtime "commons-io:commons-io:1.4"
}

uploadArchives {
    repositories {
        mavenDeployer {
            repository(url: "${mavenRepo.uri}")
        }
    }
}
"""

        when:
        run "uploadArchives"

        then:
        def mavenModule = mavenRepo.module("org.gradle.test", "publishTest", "1.9").withoutExtraChecksums()
        mavenModule.assertArtifactsPublished("publishTest-1.9.pom", "publishTest-1.9.ear")
    }
}
