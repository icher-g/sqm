package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * Named union of primary variables in a match pattern.
 */
public non-sealed interface PatternSubset extends Node {
    /**
     * Creates a pattern subset.
     *
     * @param name subset name
     * @param variables non-empty list of primary variables
     * @return immutable pattern subset
     */
    static PatternSubset of(Identifier name, List<Identifier> variables) {
        return new Impl(name, variables);
    }

    /**
     * Returns the subset name.
     *
     * @return subset name
     */
    Identifier name();

    /**
     * Returns the primary variables in source order.
     *
     * @return immutable variable list
     */
    List<Identifier> variables();

    /**
     * Accepts a node visitor.
     *
     * @param visitor visitor to accept
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitPatternSubset(this);
    }

    /**
     * Immutable pattern-subset implementation.
     *
     * @param name subset name
     * @param variables non-empty primary-variable list
     */
    record Impl(Identifier name, List<Identifier> variables) implements PatternSubset {
        /**
         * Validates and defensively copies the subset.
         *
         * @param name subset name
         * @param variables non-empty primary-variable list
         */
        public Impl {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(variables, "variables");
            if (variables.isEmpty()) {
                throw new IllegalArgumentException("Pattern subset requires at least one variable");
            }
            variables = List.copyOf(variables);
        }
    }
}
