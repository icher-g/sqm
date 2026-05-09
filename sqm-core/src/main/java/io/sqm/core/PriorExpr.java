package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Represents an Oracle-style {@code PRIOR} expression used in hierarchical queries.
 */
public non-sealed interface PriorExpr extends Expression {

    /**
     * Creates a {@code PRIOR} expression.
     *
     * @param expr expression evaluated against the parent row
     * @return prior expression
     */
    static PriorExpr of(Expression expr) {
        return new Impl(expr);
    }

    /**
     * Returns the expression evaluated against the parent row.
     *
     * @return wrapped expression
     */
    Expression expr();

    /**
     * Accepts a visitor.
     *
     * @param v visitor instance
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitPriorExpr(this);
    }

    /**
     * Default immutable implementation.
     *
     * @param expr expression evaluated against the parent row
     */
    record Impl(Expression expr) implements PriorExpr {
        /**
         * Creates a prior expression implementation.
         *
         * @param expr expression evaluated against the parent row
         */
        public Impl {
            Objects.requireNonNull(expr, "expr");
        }
    }
}
