package org.gradle.snapshotting.contexts;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import java.util.Collection;
import java.util.List;

public interface CompareStrategy {
    CompareStrategy ORDER_SENSITIVE = results -> results;
    CompareStrategy ORDER_INSENSITIVE = results -> {
        // Make sure classpath entries have their elements sorted before combining the hashes
        List<Result> sortedResults = Lists.newArrayList(results);
        sortedResults.sort(Ordering.natural().onResultOf(Result::getNormalizedPath));
        return sortedResults;
    };

    Collection<Result> sort(Collection<Result> results);
}
