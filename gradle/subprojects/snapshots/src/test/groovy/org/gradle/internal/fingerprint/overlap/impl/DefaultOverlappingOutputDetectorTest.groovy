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

package org.gradle.internal.fingerprint.overlap.impl

import com.google.common.collect.ImmutableSortedMap
import org.gradle.internal.file.FileMetadata.AccessType
import org.gradle.internal.file.impl.DefaultFileMetadata
import org.gradle.internal.fingerprint.FileCollectionFingerprint
import org.gradle.internal.fingerprint.impl.AbsolutePathFingerprintingStrategy
import org.gradle.internal.fingerprint.impl.DefaultCurrentFileCollectionFingerprint
import org.gradle.internal.hash.HashCode
import org.gradle.internal.snapshot.CompleteDirectorySnapshot
import org.gradle.internal.snapshot.FileSystemSnapshot
import org.gradle.internal.snapshot.MissingFileSnapshot
import org.gradle.internal.snapshot.RegularFileSnapshot
import spock.lang.Specification
import spock.lang.Unroll

class DefaultOverlappingOutputDetectorTest extends Specification {
    def detector = new DefaultOverlappingOutputDetector()

    def "detects no overlap when there are none"() {
        def outputFilesAfterPreviousExecution = ImmutableSortedMap.<String, FileCollectionFingerprint> of(
            "output", AbsolutePathFingerprintingStrategy.INCLUDE_MISSING.emptyFingerprint
        )
        def outputFilesBeforeExecution = ImmutableSortedMap.<String, FileSystemSnapshot> of(
            "output", FileSystemSnapshot.EMPTY
        )
        expect:
        detector.detect(outputFilesAfterPreviousExecution, outputFilesBeforeExecution) == null
    }

    def "detects overlap when there is a stale root"() {
        def staleFileAddedBetweenExecutions = new RegularFileSnapshot("/absolute/path", "path", HashCode.fromInt(1234), DefaultFileMetadata.file(0, 0, AccessType.DIRECT))
        def outputFilesAfterPreviousExecution = ImmutableSortedMap.<String, FileCollectionFingerprint>of(
            "output", AbsolutePathFingerprintingStrategy.INCLUDE_MISSING.emptyFingerprint
        )
        def outputFilesBeforeExecution = ImmutableSortedMap.<String, FileSystemSnapshot>of(
            "output", staleFileAddedBetweenExecutions
        )

        when:
        def overlaps = detector.detect(outputFilesAfterPreviousExecution, outputFilesBeforeExecution)

        then:
        overlaps.propertyName == "output"
        overlaps.overlappedFilePath == "/absolute/path"
    }

    @Unroll
    def "detects overlap when there is a stale #type in an output directory"() {
        def emptyDirectoryFingerprint = DefaultCurrentFileCollectionFingerprint.from([
            new CompleteDirectorySnapshot("/absolute", "absolute", AccessType.DIRECT, HashCode.fromInt(123), [])
        ], AbsolutePathFingerprintingStrategy.INCLUDE_MISSING)
        def directoryWithStaleBrokenSymlink = new CompleteDirectorySnapshot("/absolute", "absolute", AccessType.DIRECT, HashCode.fromInt(123), [
            staleEntry
        ])
        def outputFilesAfterPreviousExecution = ImmutableSortedMap.<String, FileCollectionFingerprint> of(
            "output", emptyDirectoryFingerprint
        )
        def outputFilesBeforeExecution = ImmutableSortedMap.<String, FileSystemSnapshot> of(
            "output", directoryWithStaleBrokenSymlink
        )

        when:
        def overlaps = detector.detect(outputFilesAfterPreviousExecution, outputFilesBeforeExecution)

        then:
        overlaps.propertyName == "output"
        overlaps.overlappedFilePath == "/absolute/path"

        where:
        type             | staleEntry
        "file"           | new RegularFileSnapshot("/absolute/path", "path", HashCode.fromInt(123), DefaultFileMetadata.file(0L, 0L, AccessType.DIRECT))
        "directory"      | new CompleteDirectorySnapshot("/absolute/path", "path", AccessType.DIRECT, HashCode.fromInt(123), [])
        "broken symlink" | new MissingFileSnapshot("/absolute/path", "path", AccessType.VIA_SYMLINK)
    }
}
