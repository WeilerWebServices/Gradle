package org.gradle.snapshotting.files;

import com.google.common.hash.HashCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PhysicalFile extends AbstractFileish implements Physical, FileishWithContents {
    private final File file;
    private final HashCode contentHash;

    public PhysicalFile(String path, PhysicalDirectory parent, File file, HashCode contentHash) {
        super(path, parent);
        this.file = file;
        this.contentHash = contentHash;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public InputStream open() throws IOException {
        return new FileInputStream(file);
    }


    @Override
    public PhysicalDirectory getParent() {
        return (PhysicalDirectory) super.getParent();
    }

    @Override
    public HashCode getContentHash() {
        return contentHash;
    }

    @Override
    public String getRelativePath() {
        return Physical.getRelativePath(getParent(), this);
    }
}
