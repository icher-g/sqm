package io.sqm.core;

import io.sqm.core.match.HintMatch;

import java.util.List;

/**
 * Shared semantic root for typed SQL hints.
 */
public sealed interface Hint extends Node permits StatementHint, TableHint {

    /**
     * Returns the canonical hint name.
     *
     * @return hint name
     */
    Identifier name();

    /**
     * Returns the ordered typed hint arguments.
     *
     * @return immutable hint arguments
     */
    List<HintArg> args();

    /**
     * Creates a matcher for this hint.
     *
     * @param <R> result type
     * @return hint matcher
     */
    default <R> HintMatch<R> matchHint() {
        return HintMatch.match(this);
    }
}