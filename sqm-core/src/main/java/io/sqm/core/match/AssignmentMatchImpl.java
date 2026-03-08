package io.sqm.core.match;

import io.sqm.core.Assignment;

import java.util.function.Function;

/**
 * Default matcher implementation for {@link Assignment}.
 *
 * @param <R> result type
 */
public final class AssignmentMatchImpl<R> implements AssignmentMatch<R> {

    private final Assignment assignment;
    private boolean matched;
    private R result;

    /**
     * Creates a matcher for the provided assignment.
     *
     * @param assignment assignment to match
     */
    public AssignmentMatchImpl(Assignment assignment) {
        this.assignment = assignment;
    }

    @Override
    public AssignmentMatch<R> assignment(Function<Assignment, R> function) {
        if (!matched) {
            result = function.apply(assignment);
            matched = true;
        }
        return this;
    }

    @Override
    public R otherwise(Function<Assignment, R> function) {
        return matched ? result : function.apply(assignment);
    }
}
