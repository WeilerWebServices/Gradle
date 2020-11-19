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

package org.gradle.api.internal.changedetection.state

import org.gradle.internal.file.FileMetadata
import org.gradle.internal.file.impl.DefaultFileMetadata
import org.gradle.internal.hash.HashCode
import org.gradle.internal.snapshot.RegularFileSnapshot
import spock.lang.Specification


class IgnoringResourceHasherTest extends Specification {
    def delegate = Mock(ResourceHasher)
    def resourceFilter = Mock(ResourceFilter)
    def hasher = new IgnoringResourceHasher(delegate, resourceFilter)
    def snapshot = new RegularFileSnapshot("path", "path", HashCode.fromInt(456), DefaultFileMetadata.file(3456, 456, FileMetadata.AccessType.DIRECT))
    def snapshotContext = new DefaultRegularFileSnapshotContext({path.split('/')}, snapshot)

    def "allows all resources when resource filter does not match"() {
        when:
        hasher.hash(snapshotContext)

        then:
        1 * resourceFilter.shouldBeIgnored(_) >> false
        1 * delegate.hash(snapshotContext)
    }

    def "filters resources when resource filter matches"() {
        when:
        def hash = hasher.hash(snapshotContext)

        then:
        1 * resourceFilter.shouldBeIgnored(_) >> true
        0 * delegate.hash(_)

        and:
        hash == null
    }
}
