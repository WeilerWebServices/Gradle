package org.gradle.snapshotting.files;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

public interface Physical extends Fileish {
    File getFile();

    static Physical of(String path, PhysicalDirectory parent, File file) {
        try {
            if (!file.exists()) {
                return new MissingPhysicalFile(path, parent, file);
            } else if (file.isDirectory()) {
                return new PhysicalDirectory(path, parent, file);
            } else {
                HashCode contentHash = Files.hash(file, Hashing.md5());
                return new PhysicalFile(path, parent, file, contentHash);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String getRelativePath(PhysicalDirectory parent, Physical current) {
        if (parent == null) {
            return current.getFile().getName();
        }
        return parent.getFile().toPath().relativize(current.getFile().toPath()).toString();
    }
}
