package org.gradle.snapshotting.cache;

import com.google.common.hash.HashCode;
import org.gradle.snapshotting.files.PhysicalFile;

public class NoOpPhysicalHashCache implements PhysicalHashCache {
    @Override
    public HashCode getCachedHashFor(PhysicalFile file) {
        return null;
    }

    @Override
    public void setCachedHashFor(PhysicalFile file, HashCode hash) {
    }
}
