package org.gradle.snapshotting.files;

import com.google.common.hash.HashCode;

public interface PhysicalSnapshot {
    Physical getFile();
    String getNormalizedPath();
    HashCode getHashCode();
}
