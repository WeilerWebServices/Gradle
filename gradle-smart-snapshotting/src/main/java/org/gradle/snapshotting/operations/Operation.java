package org.gradle.snapshotting.operations;

import org.gradle.snapshotting.SnapshotterState;
import org.gradle.snapshotting.contexts.Context;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public abstract class Operation implements Closeable {
    private Context context;

    public Operation(Context context) {
        this.context = context;
    }

    public Context getContext() {
        if (context == null) {
            throw new IllegalStateException("No context is specified");
        }
        return context;
    }

    public void setContextIfNecessary(SnapshotterState state) {
        if (this.context == null) {
            this.context = state.getContext();
        } else {
            state.setContext(this.context);
        }
    }

    // TODO: I would prefer if execute only would get the rules, not the whole state
    public abstract boolean execute(SnapshotterState state, List<Operation> dependencies) throws IOException;

    @Override
    public void close() throws IOException {
    }
}
