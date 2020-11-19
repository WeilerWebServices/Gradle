/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.execution.steps;

import com.google.common.collect.ImmutableSortedMap;
import org.gradle.caching.internal.origin.OriginMetadata;
import org.gradle.internal.Try;
import org.gradle.internal.execution.BeforeExecutionContext;
import org.gradle.internal.execution.CurrentSnapshotResult;
import org.gradle.internal.execution.OutputSnapshotter;
import org.gradle.internal.execution.Result;
import org.gradle.internal.execution.Step;
import org.gradle.internal.execution.UnitOfWork;
import org.gradle.internal.execution.history.AfterPreviousExecutionState;
import org.gradle.internal.execution.history.BeforeExecutionState;
import org.gradle.internal.fingerprint.CurrentFileCollectionFingerprint;
import org.gradle.internal.fingerprint.FileCollectionFingerprint;
import org.gradle.internal.fingerprint.impl.DefaultCurrentFileCollectionFingerprint;
import org.gradle.internal.id.UniqueId;
import org.gradle.internal.operations.BuildOperationDescriptor;
import org.gradle.internal.operations.BuildOperationExecutor;
import org.gradle.internal.operations.BuildOperationType;
import org.gradle.internal.snapshot.FileSystemSnapshot;

import java.util.Collections;

import static com.google.common.collect.ImmutableSortedMap.copyOfSorted;
import static com.google.common.collect.Maps.transformEntries;
import static org.gradle.internal.execution.impl.OutputFilterUtil.filterOutputSnapshotAfterExecution;
import static org.gradle.internal.fingerprint.impl.AbsolutePathFingerprintingStrategy.IGNORE_MISSING;

public class SnapshotOutputsStep<C extends BeforeExecutionContext> extends BuildOperationStep<C, CurrentSnapshotResult> {
    private final UniqueId buildInvocationScopeId;
    private final OutputSnapshotter outputSnapshotter;
    private final Step<? super C, ? extends Result> delegate;

    public SnapshotOutputsStep(
        BuildOperationExecutor buildOperationExecutor,
        UniqueId buildInvocationScopeId,
        OutputSnapshotter outputSnapshotter,
        Step<? super C, ? extends Result> delegate
    ) {
        super(buildOperationExecutor);
        this.buildInvocationScopeId = buildInvocationScopeId;
        this.outputSnapshotter = outputSnapshotter;
        this.delegate = delegate;
    }

    @Override
    public CurrentSnapshotResult execute(UnitOfWork work, C context) {
        Result result = delegate.execute(work, context);
        ImmutableSortedMap<String, CurrentFileCollectionFingerprint> finalOutputs = operation(
            operationContext -> {
                ImmutableSortedMap<String, CurrentFileCollectionFingerprint> outputSnapshots = captureOutputs(work, context);
                operationContext.setResult(Operation.Result.INSTANCE);
                return outputSnapshots;
            },
            BuildOperationDescriptor
                .displayName("Snapshot outputs after executing " + work.getDisplayName())
                .details(Operation.Details.INSTANCE)
        );

        OriginMetadata originMetadata = new OriginMetadata(buildInvocationScopeId.asString(), work.markExecutionTime());

        return new CurrentSnapshotResult() {
            @Override
            public ImmutableSortedMap<String, CurrentFileCollectionFingerprint> getFinalOutputs() {
                return finalOutputs;
            }

            @Override
            public OriginMetadata getOriginMetadata() {
                return originMetadata;
            }

            @Override
            public Try<ExecutionResult> getExecutionResult() {
                return result.getExecutionResult();
            }

            @Override
            public boolean isReused() {
                return false;
            }
        };
    }

    private ImmutableSortedMap<String, CurrentFileCollectionFingerprint> captureOutputs(UnitOfWork work, BeforeExecutionContext context) {
        ImmutableSortedMap<String, FileCollectionFingerprint> afterPreviousExecutionOutputFingerprints = context.getAfterPreviousExecutionState()
            .map(AfterPreviousExecutionState::getOutputFileProperties)
            .orElse(ImmutableSortedMap.of());

        ImmutableSortedMap<String, FileSystemSnapshot> beforeExecutionOutputSnapshots = context.getBeforeExecutionState()
            .map(BeforeExecutionState::getOutputFileSnapshots)
            .orElse(ImmutableSortedMap.of());

        boolean hasDetectedOverlappingOutputs = context.getBeforeExecutionState()
            .flatMap(BeforeExecutionState::getDetectedOverlappingOutputs)
            .isPresent();

        ImmutableSortedMap<String, FileSystemSnapshot> afterExecutionOutputSnapshots = outputSnapshotter.snapshotOutputs(work, context.getWorkspace());

        return copyOfSorted(transformEntries(
            afterExecutionOutputSnapshots,
            (propertyName, afterExecutionOutputSnapshot) -> {
                // This can never be null as it comes from an ImmutableMap's value
                assert afterExecutionOutputSnapshot != null;

                Iterable<FileSystemSnapshot> filteredSnapshots;
                if (hasDetectedOverlappingOutputs) {
                    FileCollectionFingerprint afterLastExecutionFingerprint = afterPreviousExecutionOutputFingerprints.get(propertyName);
                    FileSystemSnapshot beforeExecutionOutputSnapshot = beforeExecutionOutputSnapshots.get(propertyName);
                    filteredSnapshots = filterOutputSnapshotAfterExecution(afterLastExecutionFingerprint, beforeExecutionOutputSnapshot, afterExecutionOutputSnapshot);
                } else {
                    filteredSnapshots = Collections.singleton(afterExecutionOutputSnapshot);
                }
                return DefaultCurrentFileCollectionFingerprint.from(filteredSnapshots, IGNORE_MISSING);
            }
        ));
    }

    /*
     * This operation is only used here temporarily. Should be replaced with a more stable operation in the long term.
     */
    public interface Operation extends BuildOperationType<Operation.Details, Operation.Result> {
        interface Details {
            Details INSTANCE = new Details() {};
        }

        interface Result {
            Result INSTANCE = new Result() {};
        }
    }
}
