package org.gradle.snapshotting.files;

abstract public class AbstractFileish implements Fileish {
    private final String path;
    private final Fileish parent;

    public AbstractFileish(String path, Fileish parent) {
        this.path = path;
        this.parent = parent;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return path;
    }

    public Fileish getParent() {
        return parent;
    }
}
