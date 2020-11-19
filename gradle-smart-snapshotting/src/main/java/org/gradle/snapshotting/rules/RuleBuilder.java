package org.gradle.snapshotting.rules;

import org.gradle.snapshotting.contexts.Context;
import org.gradle.snapshotting.files.Fileish;
import org.gradle.snapshotting.operations.Operation;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class RuleBuilder<F extends Fileish, C extends Context> {
    private final Class<F> fileType;
    private final Class<C> context;
    private final Pattern matcher;

    private RuleBuilder(Class<F> fileType, Class<C> context, Pattern matcher) {
        this.fileType = fileType;
        this.context = context;
        this.matcher = matcher;
    }

    public static RuleBuilder<Fileish, Context> rule() {
        return new RuleBuilder<>(Fileish.class, Context.class, null);
    }

    public <CC extends Context> RuleBuilder<F, CC> in(Class<CC> context) {
        return new RuleBuilder<>(fileType, context, null);
    }

    public <CC extends Context> Rule<F, CC> in(Class<CC> context, RuleAction<? super F, ? super CC> action) {
        return in(context).action(action);
    }

    public <FF extends Fileish> RuleBuilder<FF, C> withType(Class<FF> fileType) {
        return new RuleBuilder<>(fileType, context, null);
    }

    public <FF extends Fileish> Rule<FF, C> withType(Class<FF> fileType, RuleAction<? super FF, ? super C> action) {
        return withType(fileType).action(action);
    }

    public RuleBuilder<F, C> withExtension(String extension) {
        return new RuleBuilder<>(fileType, context, Pattern.compile(".*\\." + Pattern.quote(extension)));
    }

    public Rule<F, C> withExtension(String extension, RuleAction<? super F, ? super C> action) {
        return withExtension(extension).action(action);
    }

    public RuleBuilder<F, C> matching(String pattern) {
        return new RuleBuilder<>(fileType, context, Pattern.compile(pattern));
    }

    public Rule<F, C> matching(String pattern, RuleAction<? super F, ? super C> action) {
        return matching(pattern).action(action);
    }

    public RuleBuilder<F, C> matching(Pattern pattern) {
        return new RuleBuilder<>(fileType, context, matcher);
    }

    public Rule<F, C> matching(Pattern pattern, RuleAction<? super F, ? super C> action) {
        return matching(pattern).action(action);
    }

    public Rule<F, C> action(RuleAction<? super F, ? super C> action) {
        return new Rule<F, C>(fileType, context, matcher) {
            @Override
            @SuppressWarnings("unchecked")
            public void process(F file, C context, List<Operation> operations) throws IOException {
                action.execute(file, context, operations);
            }
        };
    }

    public interface RuleAction<F extends Fileish, C extends Context> {
        void execute(Fileish file, Context context, List<Operation> operations) throws IOException;
    }
}
