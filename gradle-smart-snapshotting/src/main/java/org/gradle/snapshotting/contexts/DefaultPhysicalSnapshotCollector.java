package org.gradle.snapshotting.contexts;

import com.google.common.collect.ImmutableCollection;
import com.google.common.hash.HashCode;
import org.gradle.snapshotting.files.DefaultPhysicalSnapshot;
import org.gradle.snapshotting.files.Physical;
import org.gradle.snapshotting.files.PhysicalSnapshot;

public class DefaultPhysicalSnapshotCollector implements PhysicalSnapshotCollector {
    private final ImmutableCollection.Builder<? super PhysicalSnapshot> builder;

    public DefaultPhysicalSnapshotCollector(ImmutableCollection.Builder<? super PhysicalSnapshot> builder) {
        this.builder = builder;
    }

    @Override
    public void collectSnapshot(Physical file, String normalizedPath, HashCode hashCode) {
        builder.add(new DefaultPhysicalSnapshot(file, normalizedPath, hashCode));
    }
}
