package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Predicate that defines a primary variable in a match pattern.
 */
public non-sealed interface PatternDefinition extends Node {
    /**
     * Creates a pattern-variable definition.
     *
     * @param variable primary pattern variable
     * @param condition condition that classifies rows for the variable
     * @return immutable pattern definition
     */
    static PatternDefinition of(Identifier variable, Predicate condition) {
        return new Impl(variable, condition);
    }

    /**
     * Returns the defined primary variable.
     *
     * @return pattern variable
     */
    Identifier variable();

    /**
     * Returns the variable condition.
     *
     * @return variable condition
     */
    Predicate condition();

    /**
     * Accepts a node visitor.
     *
     * @param visitor visitor to accept
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitPatternDefinition(this);
    }

    /**
     * Immutable pattern-definition implementation.
     *
     * @param variable primary pattern variable
     * @param condition condition that classifies rows for the variable
     */
    record Impl(Identifier variable, Predicate condition) implements PatternDefinition {
        /**
         * Validates the pattern definition.
         *
         * @param variable primary pattern variable
         * @param condition condition that classifies rows for the variable
         */
        public Impl {
            Objects.requireNonNull(variable, "variable");
            Objects.requireNonNull(condition, "condition");
        }
    }
}
