package io.sqm.core;

import io.sqm.core.match.StatementMatch;

/**
 * Represents a top-level SQL statement in the SQM model.
 */
public sealed interface Statement extends Node permits DeleteStatement, InsertStatement, MergeStatement, Query, UpdateStatement {

    /**
     * Creates a new matcher for the current {@link Statement}.
     *
     * @param <R> the result type
     * @return a new {@link StatementMatch}
     */
    default <R> StatementMatch<R> matchStatement() {
        return StatementMatch.match(this);
    }
}

