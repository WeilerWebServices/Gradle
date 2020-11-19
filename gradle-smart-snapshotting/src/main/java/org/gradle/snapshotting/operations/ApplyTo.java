package org.gradle.snapshotting.operations;

import com.google.common.hash.HashCode;
import org.gradle.snapshotting.SnapshotterState;
import org.gradle.snapshotting.contexts.Context;
import org.gradle.snapshotting.files.Fileish;
import org.gradle.snapshotting.files.PhysicalFile;
import org.gradle.snapshotting.rules.Rule;
import org.gradle.snapshotting.rules.RuleMatcher;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ApplyTo extends Operation {
    private final Iterable<? extends Fileish> files;

    public ApplyTo(Fileish file) {
        this(Collections.singleton(file), null);
    }

    public ApplyTo(Iterable<? extends Fileish> files, Context context) {
        super(context);
        this.files = files;
    }

    @Override
    public boolean execute(SnapshotterState state, List<Operation> dependencies) throws IOException {
        Context context = getContext();
        for (Fileish file : files) {
            if (file instanceof PhysicalFile) {
                HashCode cachedHash = state.getHashCache().getCachedHashFor((PhysicalFile) file);
                if (cachedHash != null) {
                    getContext().recordSnapshot(file, cachedHash);
                    continue;
                }
            }

            applyRule(state.getRuleMatcher(), file, context, dependencies);
        }
        return true;
    }

    private static <F extends Fileish, C extends Context> void applyRule(RuleMatcher ruleMatcher, F file, C context, List<Operation> dependencies) throws IOException {
        Rule<? super F, ? super C> rule = ruleMatcher.match(file, context);
        rule.process(file, context, dependencies);
    }
}
