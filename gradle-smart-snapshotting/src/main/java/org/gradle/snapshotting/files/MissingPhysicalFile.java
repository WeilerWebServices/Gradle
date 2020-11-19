package org.gradle.snapshotting.files;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import java.io.File;

public class MissingPhysicalFile extends AbstractFileish implements Physical {
    public static final HashCode HASH = Hashing.md5().hashString("MISSING_FILE", Charsets.UTF_8);
    private final File file;

    public MissingPhysicalFile(String path, PhysicalDirectory parent, File file) {
        super(path, parent);
        this.file = file;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public PhysicalDirectory getParent() {
        return (PhysicalDirectory) super.getParent();
    }

    @Override
    public String getRelativePath() {
        return Physical.getRelativePath(getParent(), this);
    }
}
