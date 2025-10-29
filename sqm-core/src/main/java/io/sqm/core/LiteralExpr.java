package io.sqm.core;

import io.sqm.core.internal.LiteralExprImpl;

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
        return new LiteralExprImpl(value);
    }

    /**
     * Gets a value wrapped by this expression.
     *
     * @return a value.
     */
    Object value();
}
