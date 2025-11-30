package io.sqm.core;

/**
 * A binary arithmetic expression consisting of a left-hand side (lhs)
 * and a right-hand side (rhs) operand.
 *
 * <p>This interface is extended by specific operator types such as
 * {@code AddArithmeticExpr}, {@code SubArithmeticExpr}, {@code MulArithmeticExpr},
 * {@code DivArithmeticExpr}, and {@code ModArithmeticExpr}.</p>
 *
 * <p>Binary arithmetic expressions are strictly ordered:
 * {@code lhs op rhs}. Implementations must guarantee that both operands
 * are non-null {@link Expression} instances.</p>
 */
public sealed interface BinaryArithmeticExpr extends ArithmeticExpr
    permits AdditiveArithmeticExpr, MultiplicativeArithmeticExpr {

    /**
     * Returns the left-hand side operand of the arithmetic operation.
     *
     * @return the left operand, never {@code null}
     */
    Expression lhs();

    /**
     * Returns the right-hand side operand of the arithmetic operation.
     *
     * @return the right operand, never {@code null}
     */
    Expression rhs();
}
