package org.gradle.snapshotting.contexts;

import org.gradle.snapshotting.files.Fileish;

import java.nio.file.Paths;

public interface NormalizationStrategy {
    NormalizationStrategy ABSOLUTE = Fileish::getPath;
    NormalizationStrategy RELATIVE = Fileish::getRelativePath;
    NormalizationStrategy NAME_ONLY = file -> Paths.get(file.getRelativePath()).getFileName().toString();
    NormalizationStrategy NONE = file -> "";

    String normalize(Fileish file);
}
