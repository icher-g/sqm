package io.sqm.core.match;

import io.sqm.core.MergeAction;
import io.sqm.core.MergeDeleteAction;
import io.sqm.core.MergeDoNothingAction;
import io.sqm.core.MergeInsertAction;
import io.sqm.core.MergeUpdateAction;

import java.util.function.Function;

/**
 * Default matcher implementation for {@link MergeAction}.
 *
 * @param <R> result type
 */
public final class MergeActionMatchImpl<R> implements MergeActionMatch<R> {

    private final MergeAction action;
    private boolean matched;
    private R result;

    /**
     * Creates a matcher for the provided merge action.
     *
     * @param action merge action to match
     */
    public MergeActionMatchImpl(MergeAction action) {
        this.action = action;
    }

    @Override
    public MergeActionMatch<R> update(Function<MergeUpdateAction, R> function) {
        if (!matched && action instanceof MergeUpdateAction mergeUpdateAction) {
            result = function.apply(mergeUpdateAction);
            matched = true;
        }
        return this;
    }

    @Override
    public MergeActionMatch<R> delete(Function<MergeDeleteAction, R> function) {
        if (!matched && action instanceof MergeDeleteAction mergeDeleteAction) {
            result = function.apply(mergeDeleteAction);
            matched = true;
        }
        return this;
    }

    @Override
    public MergeActionMatch<R> doNothing(Function<MergeDoNothingAction, R> function) {
        if (!matched && action instanceof MergeDoNothingAction mergeDoNothingAction) {
            result = function.apply(mergeDoNothingAction);
            matched = true;
        }
        return this;
    }

    @Override
    public MergeActionMatch<R> insert(Function<MergeInsertAction, R> function) {
        if (!matched && action instanceof MergeInsertAction mergeInsertAction) {
            result = function.apply(mergeInsertAction);
            matched = true;
        }
        return this;
    }

    @Override
    public R otherwise(Function<MergeAction, R> function) {
        return matched ? result : function.apply(action);
    }
}
