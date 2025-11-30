package io.sqm.core.internal;

import io.sqm.core.DivArithmeticExpr;
import io.sqm.core.Expression;

/**
 * Represents a division operation of the form {@code lhs / rhs}.
 *
 * @param lhs the left operand, must not be {@code null}
 * @param rhs the right operand, must not be {@code null}
 */
public record DivArithmeticExprImpl(Expression lhs, Expression rhs) implements DivArithmeticExpr {
}
