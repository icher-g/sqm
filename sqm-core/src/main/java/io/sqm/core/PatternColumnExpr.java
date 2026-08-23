package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Column reference qualified by a primary or union pattern variable.
 */
public non-sealed interface PatternColumnExpr extends Expression {
    /**
     * Creates a pattern-variable column reference.
     *
     * @param variable primary or union pattern variable
     * @param column input-column name
     * @return immutable pattern column expression
     */
    static PatternColumnExpr of(Identifier variable, Identifier column) {
        return new Impl(variable, column);
    }

    /**
     * Returns the pattern variable.
     *
     * @return pattern variable
     */
    Identifier variable();

    /**
     * Returns the input-column name.
     *
     * @return input-column name
     */
    Identifier column();

    /**
     * Accepts a node visitor.
     *
     * @param visitor visitor to accept
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitPatternColumnExpr(this);
    }

    /**
     * Immutable pattern-column implementation.
     *
     * @param variable primary or union pattern variable
     * @param column input-column name
     */
    record Impl(Identifier variable, Identifier column) implements PatternColumnExpr {
        /**
         * Validates the pattern column.
         *
         * @param variable primary or union pattern variable
         * @param column input-column name
         */
        public Impl {
            Objects.requireNonNull(variable, "variable");
            Objects.requireNonNull(column, "column");
        }
    }
}
