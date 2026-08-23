package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Navigation to a row relative to a row-pattern expression.
 */
public non-sealed interface PatternNavigationExpr extends Expression {
    /**
     * Navigation operation.
     */
    enum Kind {
        /** First matching row for the referenced variable. */
        FIRST,
        /** Last matching row for the referenced variable. */
        LAST,
        /** Previous row relative to the current evaluation point. */
        PREV,
        /** Next row relative to the current evaluation point. */
        NEXT
    }

    /**
     * Creates a row-pattern navigation expression.
     *
     * @param kind navigation operation
     * @param expression expression evaluated at the selected row
     * @param offset optional offset expression
     * @return immutable navigation expression
     */
    static PatternNavigationExpr of(Kind kind, Expression expression, Expression offset) {
        return new Impl(kind, expression, offset);
    }

    /**
     * Returns the navigation operation.
     *
     * @return navigation operation
     */
    Kind kind();

    /**
     * Returns the expression evaluated at the selected row.
     *
     * @return navigated expression
     */
    Expression expression();

    /**
     * Returns the optional offset.
     *
     * @return offset expression, or {@code null}
     */
    Expression offset();

    /**
     * Accepts a node visitor.
     *
     * @param visitor visitor to accept
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitPatternNavigationExpr(this);
    }

    /**
     * Immutable row-pattern navigation implementation.
     *
     * @param kind navigation operation
     * @param expression expression evaluated at the selected row
     * @param offset optional offset expression
     */
    record Impl(Kind kind, Expression expression, Expression offset) implements PatternNavigationExpr {
        /**
         * Validates the navigation expression.
         *
         * @param kind navigation operation
         * @param expression expression evaluated at the selected row
         * @param offset optional offset expression
         */
        public Impl {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(expression, "expression");
        }
    }
}
