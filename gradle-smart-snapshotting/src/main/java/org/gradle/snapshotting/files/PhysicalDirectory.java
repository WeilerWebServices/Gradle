package org.gradle.snapshotting.files;

import java.io.File;

public class PhysicalDirectory extends AbstractFileish implements Physical, Directoryish {
    private final File file;

    public PhysicalDirectory(String path, PhysicalDirectory parent, File file) {
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
