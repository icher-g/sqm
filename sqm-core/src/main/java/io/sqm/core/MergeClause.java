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
        return of(matchType, null, action);
    }

    /**
     * Creates an immutable merge clause.
     *
     * @param matchType clause match type
     * @param condition optional clause predicate evaluated before the action
     * @param action branch action
     * @return immutable merge clause
     */
    static MergeClause of(MatchType matchType, Predicate condition, MergeAction action) {
        return new Impl(matchType, condition, action);
    }

    /**
     * Returns which match branch this clause belongs to.
     *
     * @return clause match type
     */
    MatchType matchType();

    /**
     * Returns the optional clause predicate evaluated before the action.
     *
     * @return clause predicate or {@code null}
     */
    Predicate condition();

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
     * Supported MERGE match branches in the shared implementation slices.
     */
    enum MatchType {
        /**
         * {@code WHEN MATCHED}.
         */
        MATCHED,
        /**
         * {@code WHEN NOT MATCHED}.
         */
        NOT_MATCHED,
        /**
         * {@code WHEN NOT MATCHED BY SOURCE}.
         */
        NOT_MATCHED_BY_SOURCE
    }

    /**
     * Default immutable implementation of {@link MergeClause}.
     *
     * @param matchType clause match type
     * @param condition optional clause predicate
     * @param action clause action
     */
    record Impl(MatchType matchType, Predicate condition, MergeAction action) implements MergeClause {
        /**
         * Creates an immutable merge clause implementation.
         */
        public Impl {
            Objects.requireNonNull(matchType, "matchType");
            Objects.requireNonNull(action, "action");
            if ((matchType == MatchType.MATCHED || matchType == MatchType.NOT_MATCHED_BY_SOURCE)
                && action instanceof MergeInsertAction) {
                throw new IllegalArgumentException(renderMatchType(matchType) + " merge clauses cannot use INSERT actions");
            }
            if (matchType == MatchType.NOT_MATCHED
                && !(action instanceof MergeInsertAction)
                && !(action instanceof MergeDoNothingAction)) {
                throw new IllegalArgumentException(renderMatchType(matchType) + " merge clauses require an INSERT or DO NOTHING action");
            }
        }

        private static String renderMatchType(MatchType matchType) {
            return switch (matchType) {
                case MATCHED -> "MATCHED";
                case NOT_MATCHED -> "NOT MATCHED";
                case NOT_MATCHED_BY_SOURCE -> "NOT MATCHED BY SOURCE";
            };
        }
    }
}
