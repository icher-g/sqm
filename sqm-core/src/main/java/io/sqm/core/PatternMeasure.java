package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Named expression projected by a {@link PatternRecognitionTable}.
 *
 * <p>A pattern measure always has an alias because the alias defines the
 * output column produced by the relation transform.</p>
 */
public non-sealed interface PatternMeasure extends Node {
    /**
     * Creates a pattern measure.
     *
     * @param expression measure expression
     * @param alias required output-column alias
     * @return immutable pattern measure
     */
    static PatternMeasure of(Expression expression, Identifier alias) {
        return new Impl(expression, alias);
    }

    /**
     * Returns the measure expression.
     *
     * @return measure expression
     */
    Expression expression();

    /**
     * Returns the required output-column alias.
     *
     * @return measure alias
     */
    Identifier alias();

    /**
     * Accepts a node visitor.
     *
     * @param visitor visitor to accept
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitPatternMeasure(this);
    }

    /**
     * Immutable pattern-measure implementation.
     *
     * @param expression measure expression
     * @param alias required output-column alias
     */
    record Impl(Expression expression, Identifier alias) implements PatternMeasure {
        /**
         * Validates the pattern measure.
         *
         * @param expression measure expression
         * @param alias required output-column alias
         */
        public Impl {
            Objects.requireNonNull(expression, "expression");
            Objects.requireNonNull(alias, "alias");
        }
    }
}
