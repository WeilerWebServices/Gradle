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

package org.gradle.internal.snapshot;

import org.gradle.internal.file.FileMetadata.AccessType;
import org.gradle.internal.hash.HashCode;
import org.gradle.internal.hash.Hasher;
import org.gradle.internal.hash.Hashing;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.gradle.internal.snapshot.MerkleDirectorySnapshotBuilder.EmptyDirectoryHandlingStrategy.EXCLUDE_EMPTY_DIRS;

public class MerkleDirectorySnapshotBuilder {
    private static final HashCode DIR_SIGNATURE = Hashing.signature("DIR");

    private final Deque<Directory> directoryStack = new ArrayDeque<>();
    private final boolean sortingRequired;
    private CompleteFileSystemLocationSnapshot result;

    public static MerkleDirectorySnapshotBuilder sortingRequired() {
        return new MerkleDirectorySnapshotBuilder(true);
    }

    public static MerkleDirectorySnapshotBuilder noSortingRequired() {
        return new MerkleDirectorySnapshotBuilder(false);
    }

    private MerkleDirectorySnapshotBuilder(boolean sortingRequired) {
        this.sortingRequired = sortingRequired;
    }

    public void enterDirectory(CompleteDirectorySnapshot directorySnapshot, EmptyDirectoryHandlingStrategy emptyDirectoryHandlingStrategy) {
        enterDirectory(directorySnapshot.getAccessType(), directorySnapshot.getAbsolutePath(), directorySnapshot.getName(), emptyDirectoryHandlingStrategy);
    }

    public void enterDirectory(AccessType accessType, String absolutePath, String name, EmptyDirectoryHandlingStrategy emptyDirectoryHandlingStrategy) {
        directoryStack.addLast(new Directory(accessType, absolutePath, name, emptyDirectoryHandlingStrategy));
    }

    public void visitLeafElement(FileSystemLeafSnapshot snapshot) {
        collectEntry(snapshot);
    }

    public boolean leaveDirectory() {
        CompleteFileSystemLocationSnapshot snapshot = directoryStack.removeLast().fold();
        if (snapshot == null) {
            return false;
        }
        collectEntry(snapshot);
        return true;
    }

    private void collectEntry(CompleteFileSystemLocationSnapshot snapshot) {
        Directory directory = directoryStack.peekLast();
        if (directory != null) {
            directory.collectEntry(snapshot);
        } else {
            assert result == null;
            result = snapshot;
        }
    }

    @Nullable
    public CompleteFileSystemLocationSnapshot getResult() {
        return result;
    }

    public enum EmptyDirectoryHandlingStrategy {
        INCLUDE_EMPTY_DIRS,
        EXCLUDE_EMPTY_DIRS
    }

    private class Directory {
        private final AccessType accessType;
        private final String absolutePath;
        private final String name;
        private final List<CompleteFileSystemLocationSnapshot> children;
        private final EmptyDirectoryHandlingStrategy emptyDirectoryHandlingStrategy;

        public Directory(AccessType accessType, String absolutePath, String name, EmptyDirectoryHandlingStrategy emptyDirectoryHandlingStrategy) {
            this.accessType = accessType;
            this.absolutePath = absolutePath;
            this.name = name;
            this.children = new ArrayList<>();
            this.emptyDirectoryHandlingStrategy = emptyDirectoryHandlingStrategy;
        }

        public void collectEntry(CompleteFileSystemLocationSnapshot snapshot) {
            children.add(snapshot);
        }

        @Nullable
        public CompleteDirectorySnapshot fold() {
            if (emptyDirectoryHandlingStrategy == EXCLUDE_EMPTY_DIRS && children.isEmpty()) {
                return null;
            }
            if (sortingRequired) {
                children.sort(CompleteFileSystemLocationSnapshot.BY_NAME);
            }
            Hasher hasher = Hashing.newHasher();
            hasher.putHash(DIR_SIGNATURE);
            for (CompleteFileSystemLocationSnapshot child : children) {
                hasher.putString(child.getName());
                hasher.putHash(child.getHash());
            }
            return new CompleteDirectorySnapshot(absolutePath, name, accessType, hasher.hash(), children);
        }
    }
}
