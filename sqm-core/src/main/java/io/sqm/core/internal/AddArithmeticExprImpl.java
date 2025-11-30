package io.sqm.core.internal;

import io.sqm.core.AddArithmeticExpr;
import io.sqm.core.Expression;

/**
 * Represents an addition operation of the form {@code lhs + rhs}.
 *
 * @param lhs the left operand, must not be {@code null}
 * @param rhs the right operand, must not be {@code null}
 */
public record AddArithmeticExprImpl(Expression lhs, Expression rhs) implements AddArithmeticExpr {
}
