package io.sqm.core.match;

import io.sqm.core.MergeAction;
import io.sqm.core.MergeDeleteAction;
import io.sqm.core.MergeDoNothingAction;
import io.sqm.core.MergeInsertAction;
import io.sqm.core.MergeUpdateAction;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link MergeAction} subtypes.
 *
 * @param <R> the result type produced by the match
 */
public interface MergeActionMatch<R> extends Match<MergeAction, R> {

    /**
     * Creates a new matcher for the given {@link MergeAction}.
     *
     * @param action merge action to match
     * @param <R> result type
     * @return a new matcher
     */
    static <R> MergeActionMatch<R> match(MergeAction action) {
        return new MergeActionMatchImpl<>(action);
    }

    /**
     * Registers a handler for {@link MergeUpdateAction} actions.
     *
     * @param function update-action handler
     * @return {@code this} for fluent chaining
     */
    MergeActionMatch<R> update(Function<MergeUpdateAction, R> function);

    /**
     * Registers a handler for {@link MergeDeleteAction} actions.
     *
     * @param function delete-action handler
     * @return {@code this} for fluent chaining
     */
    MergeActionMatch<R> delete(Function<MergeDeleteAction, R> function);

    /**
     * Registers a handler for {@link MergeDoNothingAction} actions.
     *
     * @param function do-nothing-action handler
     * @return {@code this} for fluent chaining
     */
    MergeActionMatch<R> doNothing(Function<MergeDoNothingAction, R> function);

    /**
     * Registers a handler for {@link MergeInsertAction} actions.
     *
     * @param function insert-action handler
     * @return {@code this} for fluent chaining
     */
    MergeActionMatch<R> insert(Function<MergeInsertAction, R> function);
}
