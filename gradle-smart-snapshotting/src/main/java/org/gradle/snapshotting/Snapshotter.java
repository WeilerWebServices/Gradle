package org.gradle.snapshotting;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.hash.HashCode;
import org.gradle.snapshotting.cache.PhysicalHashCache;
import org.gradle.snapshotting.contexts.CachingCollector;
import org.gradle.snapshotting.contexts.Context;
import org.gradle.snapshotting.contexts.DefaultPhysicalSnapshotCollector;
import org.gradle.snapshotting.contexts.PhysicalSnapshotCollector;
import org.gradle.snapshotting.files.Fileish;
import org.gradle.snapshotting.files.Physical;
import org.gradle.snapshotting.files.PhysicalSnapshot;
import org.gradle.snapshotting.operations.ApplyTo;
import org.gradle.snapshotting.operations.Operation;
import org.gradle.snapshotting.rules.CachingRuleMatcher;
import org.gradle.snapshotting.rules.Rule;
import org.gradle.snapshotting.rules.RuleMatcher;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

// TODO: Handle empty directories
// TODO: Handle junk files on classpaths, and in WAR files
public class Snapshotter {
    private final PhysicalHashCache hashCache;

    public Snapshotter(PhysicalHashCache hashCache) {
        this.hashCache = hashCache;
    }

    public HashCode snapshot(Collection<? extends File> files, Context context, Iterable<? extends Rule<?, ?>> rules, ImmutableCollection.Builder<PhysicalSnapshot> physicalSnapshots) throws IOException {
        process(files.stream()
                .map(file -> Physical.of(file.getAbsolutePath(), null, file))
                .collect(Collectors.toList()), context, rules, hashCache);
        PhysicalSnapshotCollector collector = new CachingCollector(
            hashCache,
            new DefaultPhysicalSnapshotCollector(physicalSnapshots)
        );
        return context.fold(collector);
    }

    private void process(Collection<? extends Fileish> files, Context rootContext, Iterable<? extends Rule<?, ?>> rules,         PhysicalHashCache hashCache) throws IOException {
        Deque<Operation> queue = Queues.newArrayDeque();
        RuleMatcher ruleMatcher = new CachingRuleMatcher(rules);
        SnapshotterState state = new SnapshotterState(ruleMatcher, hashCache, rootContext);
        queue.addLast(new ApplyTo(files, rootContext));

        List<Operation> dependencies = Lists.newArrayList();

        while (!queue.isEmpty()) {
            Operation operation = queue.peek();
            operation.setContextIfNecessary(state);

            dependencies.clear();
            boolean done = operation.execute(state, dependencies);

            int dependencyCount = dependencies.size();
            if (done || dependencyCount == 0) {
                operation.close();
                queue.remove();
            }

            for (int idx = dependencyCount - 1; idx >= 0; idx--) {
                queue.push(dependencies.get(idx));
            }
        }
    }
}
