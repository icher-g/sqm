package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Output-cardinality and empty-match behavior for pattern recognition.
 */
public non-sealed interface RowsPerMatch extends Node {
    /**
     * Output cardinality for each recognized match.
     */
    enum Mode {
        /** Produces one result row per match. */
        ONE,
        /** Produces all participating result rows per match. */
        ALL
    }

    /**
     * Handling of empty or unmatched rows when all rows are produced.
     */
    enum EmptyMatchHandling {
        /** Uses the dialect default. */
        DEFAULT,
        /** Includes a row for an empty match. */
        SHOW_EMPTY,
        /** Omits rows for empty matches. */
        OMIT_EMPTY,
        /** Includes unmatched input rows. */
        WITH_UNMATCHED
    }

    /**
     * Creates a rows-per-match specification.
     *
     * @param mode output cardinality
     * @param emptyMatchHandling empty/unmatched-row behavior
     * @return immutable rows-per-match specification
     */
    static RowsPerMatch of(Mode mode, EmptyMatchHandling emptyMatchHandling) {
        return new Impl(mode, emptyMatchHandling);
    }

    /**
     * Returns the output-cardinality mode.
     *
     * @return output-cardinality mode
     */
    Mode mode();

    /**
     * Returns the empty/unmatched-row behavior.
     *
     * @return empty-match handling
     */
    EmptyMatchHandling emptyMatchHandling();

    /**
     * Accepts a node visitor.
     *
     * @param visitor visitor to accept
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitRowsPerMatch(this);
    }

    /**
     * Immutable rows-per-match implementation.
     *
     * @param mode output cardinality
     * @param emptyMatchHandling empty/unmatched-row behavior
     */
    record Impl(Mode mode, EmptyMatchHandling emptyMatchHandling) implements RowsPerMatch {
        /**
         * Validates the rows-per-match shape.
         *
         * @param mode output cardinality
         * @param emptyMatchHandling empty/unmatched-row behavior
         */
        public Impl {
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(emptyMatchHandling, "emptyMatchHandling");
            if (mode != Mode.ALL && emptyMatchHandling != EmptyMatchHandling.DEFAULT) {
                throw new IllegalArgumentException("Empty-match handling requires ALL ROWS PER MATCH");
            }
        }
    }
}
