package org.gradle.snapshotting.cache;

import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import org.gradle.snapshotting.files.PhysicalFile;

import java.util.Map;

public class SimplePhysicalHashCache implements PhysicalHashCache {
    private final Map<HashCode, HashCode> cache = Maps.newHashMap();

    public HashCode getCachedHashFor(PhysicalFile file) {
        return cache.get(file.getContentHash());
    }

    public void setCachedHashFor(PhysicalFile file, HashCode hash) {
        cache.put(file.getContentHash(), hash);
    }
}
