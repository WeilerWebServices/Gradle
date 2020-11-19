package org.gradle.snapshotting.files;

import com.google.common.hash.HashCode;

import java.io.IOException;
import java.io.InputStream;

public interface FileishWithContents extends Fileish {
    InputStream open() throws IOException;

    HashCode getContentHash() throws IOException;
}
