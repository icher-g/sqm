package io.sqm.core.internal;

import io.sqm.core.Expression;
import io.sqm.core.ModArithmeticExpr;

/**
 * Represents a modulo operation of the form {@code lhs % rhs}.
 *
 * <p>The rendering of this operator may vary between SQL dialects
 * (for example, {@code MOD(lhs, rhs)} vs {@code lhs % rhs}).</p>
 *
 * @param lhs the left operand, must not be {@code null}
 * @param rhs the right operand, must not be {@code null}
 */
public record ModArithmeticExprImpl(Expression lhs, Expression rhs) implements ModArithmeticExpr {
}
