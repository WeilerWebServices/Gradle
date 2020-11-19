package org.gradle.snapshotting.contexts;

import com.google.common.hash.HashCode;
import org.gradle.snapshotting.cache.PhysicalHashCache;
import org.gradle.snapshotting.files.Physical;
import org.gradle.snapshotting.files.PhysicalFile;

public class CachingCollector implements PhysicalSnapshotCollector {
    private final PhysicalHashCache hashCache;
    private final PhysicalSnapshotCollector delegate;

    public CachingCollector(PhysicalHashCache hashCache, PhysicalSnapshotCollector delegate) {
        this.hashCache = hashCache;
        this.delegate = delegate;
    }

    @Override
    public void collectSnapshot(Physical file, String normalizedPath, HashCode hashCode){
        if (file instanceof PhysicalFile) {
            hashCache.setCachedHashFor((PhysicalFile) file, hashCode);
        }
        delegate.collectSnapshot(file, normalizedPath, hashCode);
    }
}
