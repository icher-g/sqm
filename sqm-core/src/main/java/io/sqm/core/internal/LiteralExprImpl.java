package io.sqm.core.internal;

import io.sqm.core.Expression;
import io.sqm.core.LiteralExpr;

/**
 * Implements a literal expression.
 *
 * @param value a value to be wrapped by the expression.
 */
public record LiteralExprImpl(Object value) implements LiteralExpr {

    /**
     * Validates that value is not an Expression.
     *
     * @param value a value to check.
     */
    public LiteralExprImpl {
        if (value instanceof Expression)
            throw new IllegalArgumentException("literal cannot be expression.");
    }
}
