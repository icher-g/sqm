package io.sqm.core.match;

import io.sqm.core.Assignment;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link Assignment}.
 *
 * @param <R> result type
 */
public interface AssignmentMatch<R> extends Match<Assignment, R> {

    /**
     * Creates a matcher for the given assignment.
     *
     * @param assignment assignment to match
     * @param <R> result type
     * @return matcher instance
     */
    static <R> AssignmentMatch<R> match(Assignment assignment) {
        return new AssignmentMatchImpl<>(assignment);
    }

    /**
     * Registers a handler for the assignment.
     *
     * @param function handler to execute
     * @return this matcher
     */
    AssignmentMatch<R> assignment(Function<Assignment, R> function);
}
