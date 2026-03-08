package io.sqm.core;

import io.sqm.core.match.InsertSourceMatch;

/**
 * Represents a data source used by an {@link InsertStatement}.
 * <p>
 * Neutral INSERT sources currently support either row-value input
 * ({@code VALUES (...)}) via {@link RowValues}, or query input via {@link Query}.
 * </p>
 */
public sealed interface InsertSource extends Node permits Query, RowValues {

    /**
     * Creates a matcher for the current insert source.
     *
     * @param <R> result type
     * @return a matcher for this source
     */
    default <R> InsertSourceMatch<R> matchInsertSource() {
        return InsertSourceMatch.match(this);
    }
}