package org.gradle.snapshotting;

import org.gradle.snapshotting.cache.PhysicalHashCache;
import org.gradle.snapshotting.contexts.Context;
import org.gradle.snapshotting.rules.RuleMatcher;

public class SnapshotterState {
    private final RuleMatcher ruleMatcher;
    private final PhysicalHashCache hashCache;
    private Context context;

    public SnapshotterState(RuleMatcher ruleMatcher, PhysicalHashCache hashCache, Context context) {
        this.ruleMatcher = ruleMatcher;
        this.hashCache = hashCache;
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public RuleMatcher getRuleMatcher() {
        return ruleMatcher;
    }

    public PhysicalHashCache getHashCache() {
        return hashCache;
    }
}
