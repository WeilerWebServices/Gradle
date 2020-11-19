package org.gradle.snapshotting.files;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

public interface Directoryish extends Fileish {
    HashCode HASH = Hashing.md5().hashString("DIRECTORY", Charsets.UTF_8);
}
