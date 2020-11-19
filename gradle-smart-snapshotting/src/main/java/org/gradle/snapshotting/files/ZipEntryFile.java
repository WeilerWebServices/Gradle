package org.gradle.snapshotting.files;

import com.google.common.base.Preconditions;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import org.apache.commons.io.input.CloseShieldInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import static com.google.common.hash.Funnels.asOutputStream;
import static com.google.common.hash.Hashing.md5;
import static com.google.common.io.ByteStreams.copy;

public class ZipEntryFile extends AbstractFileish implements FileishWithContents {
    private final ZipInputStream inputStream;
    private boolean used;

    public ZipEntryFile(String path, Fileish zipFile, ZipInputStream inputStream) {
        super(path, zipFile);
        this.inputStream = inputStream;
    }

    @Override
    public InputStream open() throws IOException {
        ensureUnused();
        return new CloseShieldInputStream(inputStream);
    }

    @Override
    public HashCode getContentHash() throws IOException {
        ensureUnused();
        Hasher hasher = md5().newHasher();
        copy(inputStream, asOutputStream(hasher));
        return hasher.hash();
    }

    private void ensureUnused() {
        Preconditions.checkState(!used, "Zip file entry cannot be opened twice");
        used = true;
    }

    @Override
    public String getRelativePath() {
        return getPath();
    }
}
