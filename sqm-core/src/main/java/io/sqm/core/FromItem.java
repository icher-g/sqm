package io.sqm.core;

import io.sqm.core.match.FromItemMatch;

/**
 * An interface to represent a FROM statement.
 */
public sealed interface FromItem extends Node permits DialectFromItem, Join, TableRef {

    /**
     * Creates a new matcher for the current {@link FromItem}.
     *
     * @param <R> the result type
     * @return a new {@code FromItemMatch}.
     */
    default <R> FromItemMatch<R> matchFromItem() {
        return FromItemMatch.match(this);
    }
}
