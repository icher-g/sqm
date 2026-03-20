package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Models a single {@code WHEN ... THEN ...} branch inside a {@code MERGE} statement.
 */
public non-sealed interface MergeClause extends Node {

    /**
     * Creates an immutable merge clause.
     *
     * @param matchType clause match type
     * @param action branch action
     * @return immutable merge clause
     */
    static MergeClause of(MatchType matchType, MergeAction action) {
        return new Impl(matchType, action);
    }

    /**
     * Returns which match branch this clause belongs to.
     *
     * @return clause match type
     */
    MatchType matchType();

    /**
     * Returns the action executed by this clause.
     *
     * @return merge action
     */
    MergeAction action();

    /**
     * Accepts a {@link NodeVisitor}.
     *
     * @param visitor visitor instance
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitMergeClause(this);
    }

    /**
     * Supported MERGE match branches in the first implementation slice.
     */
    enum MatchType {
        /**
         * {@code WHEN MATCHED}.
         */
        MATCHED,
        /**
         * {@code WHEN NOT MATCHED}.
         */
        NOT_MATCHED
    }

    /**
     * Default immutable implementation of {@link MergeClause}.
     *
     * @param matchType clause match type
     * @param action clause action
     */
    record Impl(MatchType matchType, MergeAction action) implements MergeClause {
        /**
         * Creates an immutable merge clause implementation.
         */
        public Impl {
            Objects.requireNonNull(matchType, "matchType");
            Objects.requireNonNull(action, "action");
            if (matchType == MatchType.MATCHED && action instanceof MergeInsertAction) {
                throw new IllegalArgumentException("MATCHED merge clauses cannot use INSERT actions");
            }
            if (matchType == MatchType.NOT_MATCHED && !(action instanceof MergeInsertAction)) {
                throw new IllegalArgumentException("NOT MATCHED merge clauses require an INSERT action");
            }
        }
    }
}
