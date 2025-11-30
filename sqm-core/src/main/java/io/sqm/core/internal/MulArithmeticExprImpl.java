package io.sqm.core.internal;

import io.sqm.core.Expression;
import io.sqm.core.MulArithmeticExpr;

/**
 * Represents a multiplication operation of the form {@code lhs * rhs}.
 *
 * @param lhs the left operand, must not be {@code null}
 * @param rhs the right operand, must not be {@code null}
 */
public record MulArithmeticExprImpl(Expression lhs, Expression rhs) implements MulArithmeticExpr {
}
