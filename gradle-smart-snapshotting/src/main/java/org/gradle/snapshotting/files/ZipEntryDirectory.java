package org.gradle.snapshotting.files;

public class ZipEntryDirectory extends AbstractFileish implements Directoryish {
    public ZipEntryDirectory(String path, Fileish zipFile) {
        super(path, zipFile);
    }

    @Override
    public String getRelativePath() {
        return getPath();
    }
}
