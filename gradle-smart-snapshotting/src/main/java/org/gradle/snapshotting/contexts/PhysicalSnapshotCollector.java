package org.gradle.snapshotting.contexts;

import com.google.common.hash.HashCode;
import org.gradle.snapshotting.files.Physical;

public interface PhysicalSnapshotCollector {
    void collectSnapshot(Physical file, String normalizedPath, HashCode hashCode);
}
