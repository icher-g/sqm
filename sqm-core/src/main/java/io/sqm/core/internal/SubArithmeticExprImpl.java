package io.sqm.core.internal;

import io.sqm.core.Expression;
import io.sqm.core.SubArithmeticExpr;

/**
 * Represents a subtraction operation of the form {@code lhs - rhs}.
 *
 * @param lhs the left operand, must not be {@code null}
 * @param rhs the right operand, must not be {@code null}
 */
public record SubArithmeticExprImpl(Expression lhs, Expression rhs) implements SubArithmeticExpr {
}
