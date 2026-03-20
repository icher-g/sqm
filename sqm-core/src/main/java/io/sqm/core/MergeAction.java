package io.sqm.core;

import io.sqm.core.match.MergeActionMatch;

/**
 * Base type for SQL {@code MERGE} actions executed after a match decision.
 */
public sealed interface MergeAction extends Node permits MergeDeleteAction, MergeInsertAction, MergeUpdateAction {

    /**
     * Creates a new matcher for the current {@link MergeAction}.
     *
     * @param <R> the result type
     * @return a new matcher
     */
    default <R> MergeActionMatch<R> matchMergeAction() {
        return MergeActionMatch.match(this);
    }
}
