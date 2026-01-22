package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Represents a literal.
 */
public non-sealed interface LiteralExpr extends Expression {
    /**
     * Creates a literal expression.
     *
     * @param value a value.
     * @return A newly created instance of a literal expression.
     */
    static LiteralExpr of(Object value) {
        return new Impl(value);
    }

    /**
     * Gets a value wrapped by this expression.
     *
     * @return a value.
     */
    Object value();

    /**
     * Accepts a {@link NodeVisitor} and dispatches control to the
     * visitor method corresponding to the concrete subtype
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type returned by the visitor
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitLiteralExpr(this);
    }

    /**
     * Implements a literal expression.
     *
     * @param value a value to be wrapped by the expression.
     */
    record Impl(Object value) implements LiteralExpr {

        /**
         * Validates that value is not an Expression.
         *
         * @param value a value to check.
         */
        public Impl {
            if (value instanceof Expression)
                throw new IllegalArgumentException("literal cannot be expression.");
        }
    }
}
