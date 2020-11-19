package org.gradle.snapshotting.files;

import com.google.common.hash.HashCode;

public class DefaultPhysicalSnapshot implements PhysicalSnapshot {
    private final Physical file;
    private final String normalizedPath;
    private final HashCode hashCode;

    public DefaultPhysicalSnapshot(Physical file, String normalizedPath, HashCode hashCode) {
        this.file = file;
        this.normalizedPath = normalizedPath;
        this.hashCode = hashCode;
    }

    @Override
    public Physical getFile() {
        return file;
    }

    @Override
    public HashCode getHashCode() {
        return hashCode;
    }

    @Override
    public String getNormalizedPath() {
        return normalizedPath;
    }

    @Override
    public String toString() {
        String path = file.getRelativePath();
        return path + (
                path.equals(normalizedPath) ? "" : " ('" + normalizedPath + "')"
        ) + ": " + hashCode;
    }
}
