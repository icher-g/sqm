package io.sqm.core;

import io.sqm.core.match.StatementMatch;

import java.util.List;

/**
 * Represents a top-level SQL statement in the SQM model.
 */
public sealed interface Statement extends Node permits DeleteStatement, InsertStatement, MergeStatement, Query, UpdateStatement {

    /**
     * Returns typed hints attached to this statement.
     *
     * @return immutable typed hint list
     */
    default List<StatementHint> hints() {
        return List.of();
    }

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

