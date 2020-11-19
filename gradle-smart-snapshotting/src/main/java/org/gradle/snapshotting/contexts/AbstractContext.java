package org.gradle.snapshotting.contexts;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.gradle.snapshotting.files.Fileish;

import java.util.Collection;
import java.util.Map;

public abstract class AbstractContext implements Context {
    private final Map<String, Result> results = Maps.newLinkedHashMap();
    private final NormalizationStrategy normalizationStrategy;
    private final CompareStrategy compareStrategy;

    protected AbstractContext() {
        this(Fileish::getPath, CompareStrategy.ORDER_INSENSITIVE);
    }

    protected AbstractContext(NormalizationStrategy normalizationStrategy, CompareStrategy compareStrategy) {
        this.normalizationStrategy = normalizationStrategy;
        this.compareStrategy = compareStrategy;
    }

    @Override
    public Class<? extends Context> getType() {
        return getClass();
    }

    @Override
    public void recordSnapshot(Fileish file, HashCode hash) {
        results.put(file.getPath(), new SnapshotResult(file, normalize(file), hash));
    }

    @Override
    public <C extends Context> C recordSubContext(Fileish file, Class<C> type) {
        String path = file.getPath();
        Result result = results.get(path);
        C subContext;
        if (result == null) {
            try {
                subContext = type.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            final SubContextResult subContextResult = new SubContextResult(file, normalize(file), subContext);
            results.put(path, subContextResult);
        } else if (result instanceof SubContextResult) {
            Context resultSubContext = ((SubContextResult) result).getSubContext();
            subContext = type.cast(resultSubContext);
        } else {
            throw new IllegalStateException("Already has a non-context entry under path " + path);
        }
        return subContext;
    }

    @Override
    public final HashCode fold(PhysicalSnapshotCollector physicalSnapshots) {
        return fold(results.values(), physicalSnapshots);
    }

    private String normalize(Fileish file) {
        return normalizationStrategy.normalize(file);
    }

    private HashCode fold(Collection<Result> results, PhysicalSnapshotCollector physicalSnapshots) {
        Hasher hasher = Hashing.md5().newHasher();
        for (Result result : compareStrategy.sort(results)) {
            hasher.putString(result.getNormalizedPath(), Charsets.UTF_8);
            hasher.putBytes(result.fold(physicalSnapshots).asBytes());
        }
        return hasher.hash();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
