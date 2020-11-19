package org.gradle.snapshotting.rules;

import org.gradle.snapshotting.contexts.Context;
import org.gradle.snapshotting.files.Fileish;
import org.gradle.snapshotting.operations.Operation;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public abstract class Rule<F extends Fileish, C extends Context> {
    private final Class<F> fileType;
    private final Class<C> contextType;
    private final Pattern pathMatcher;

    public Rule(Class<F> fileType, Class<C> contextType, Pattern pathMatcher) {
        this.contextType = contextType;
        this.fileType = fileType;
        this.pathMatcher = pathMatcher;
    }

    public Class<F> getFileType() {
        return fileType;
    }

    public Class<C> getContextType() {
        return contextType;
    }

    public Pattern getPathMatcher() {
        return pathMatcher;
    }

    public abstract void process(F file, C context, List<Operation> operations) throws IOException;
}
