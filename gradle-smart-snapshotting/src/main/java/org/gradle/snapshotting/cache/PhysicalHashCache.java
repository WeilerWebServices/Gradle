package org.gradle.snapshotting.cache;

import com.google.common.hash.HashCode;
import org.gradle.snapshotting.files.PhysicalFile;

public interface PhysicalHashCache {
    HashCode getCachedHashFor(PhysicalFile file);
    void setCachedHashFor(PhysicalFile file, HashCode hash);
}
