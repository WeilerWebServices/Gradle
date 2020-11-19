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

package org.gradle.jvm.internal

import org.gradle.jvm.JvmLibrarySpec
import org.gradle.platform.base.component.BaseComponentFixtures
import org.gradle.platform.base.internal.DefaultComponentSpecIdentifier
import spock.lang.Specification

class DefaultJvmLibrarySpecTest extends Specification {
    def libraryId = new DefaultComponentSpecIdentifier(":project-path", "jvm-lib")

    def "library has name and path"() {
        when:
        def library = createJvmLibrarySpec()

        then:
        library.name == "jvm-lib"
        library.projectPath == ":project-path"
        library.displayName == "JVM library 'jvm-lib'"
    }

    private JvmLibrarySpec createJvmLibrarySpec() {
        BaseComponentFixtures.create(JvmLibrarySpec, DefaultJvmLibrarySpec, libraryId)
    }
}
