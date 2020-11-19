package org.gradle.snapshotting.rules;

import org.gradle.snapshotting.contexts.Context;
import org.gradle.snapshotting.files.Fileish;

public interface RuleMatcher {
    <F extends Fileish, C extends Context> Rule<? super F, ? super C> match(F file, C context);
}
