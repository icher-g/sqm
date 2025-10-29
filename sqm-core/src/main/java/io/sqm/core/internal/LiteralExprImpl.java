package io.sqm.core.internal;

import io.sqm.core.LiteralExpr;

/**
 * Implements a literal expression.
 *
 * @param value a value to be wrapped by the expression.
 */
public record LiteralExprImpl(Object value) implements LiteralExpr {
}
