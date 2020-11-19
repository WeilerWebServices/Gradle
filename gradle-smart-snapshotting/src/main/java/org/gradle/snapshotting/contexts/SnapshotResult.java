package org.gradle.snapshotting.contexts;

import com.google.common.hash.HashCode;
import org.gradle.snapshotting.files.Fileish;

public class SnapshotResult extends Result {
    private final HashCode hashCode;

    public SnapshotResult(Fileish file, String normalizedPath, HashCode hashCode) {
        super(file, normalizedPath);
        this.hashCode = hashCode;
    }

    @Override
    public HashCode foldInternal(PhysicalSnapshotCollector physicalSnapshots) {
        return hashCode;
    }
}
