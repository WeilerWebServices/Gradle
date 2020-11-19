package org.gradle.snapshotting.rules;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import org.gradle.snapshotting.contexts.Context;
import org.gradle.snapshotting.files.Fileish;

import java.util.List;

public class CachingRuleMatcher implements RuleMatcher {
    private final Iterable<? extends Rule<?, ?>> rules;
    private final LoadingCache<Key, List<Rule<?, ?>>> rulesCache = CacheBuilder.newBuilder()
        .build(new CacheLoader<Key, List<Rule<?, ?>>>() {
            @Override
            public List<Rule<?, ?>> load(Key key) throws Exception {
                ImmutableList.Builder<Rule<?, ?>> matches = ImmutableList.builder();
                for (Rule<?, ?> rule : rules) {
                    if (rule.getContextType().isAssignableFrom(key.contextType)
                        && rule.getFileType().isAssignableFrom(key.fileType)) {
                        matches.add(rule);
                    }
                }
                return matches.build();
            }
        });

    public CachingRuleMatcher(Iterable<? extends Rule<?, ?>> rules) {
        this.rules = rules;
    }

    @Override
    public <F extends Fileish, C extends Context> Rule<? super F, ? super C> match(F file, C context) {
        for (Rule<?, ?> rule : rulesCache.getUnchecked(new Key(file.getClass(), context.getType()))) {
            if (rule.getPathMatcher() == null || rule.getPathMatcher().matcher(file.getPath()).matches()) {
                //noinspection unchecked
                return (Rule<F, C>) rule;
            }
        }
        throw new IllegalStateException(String.format("Cannot find matching rule for %s in context %s", file, context));
    }

    private static class Key {
        private final Class<? extends Fileish> fileType;
        private final Class<? extends Context> contextType;
        private final int hashCode;

        public Key(Class<? extends Fileish> fileType, Class<? extends Context> contextType) {
            this.contextType = contextType;
            this.fileType = fileType;
            this.hashCode = fileType.hashCode() * 31 + contextType.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            return fileType.equals(key.fileType) && contextType.equals(key.contextType);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
