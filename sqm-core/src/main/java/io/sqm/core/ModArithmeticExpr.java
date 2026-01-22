package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Represents a modulo operation of the form {@code lhs % rhs}.
 *
 * <p>The rendering of this operator may vary between SQL dialects
 * (for example, {@code MOD(lhs, rhs)} vs {@code lhs % rhs}).</p>
 */
public non-sealed interface ModArithmeticExpr extends MultiplicativeArithmeticExpr {

    /**
     * Creates a new modulo expression.
     *
     * @param lhs the left operand, must not be {@code null}
     * @param rhs the right operand, must not be {@code null}
     * @return a new {@code ModArithmeticExpr} instance
     */
    static ModArithmeticExpr of(Expression lhs, Expression rhs) {
        return new Impl(lhs, rhs);
    }

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
        return v.visitModArithmeticExpr(this);
    }

    /**
     * Represents a modulo operation of the form {@code lhs % rhs}.
     *
     * <p>The rendering of this operator may vary between SQL dialects
     * (for example, {@code MOD(lhs, rhs)} vs {@code lhs % rhs}).</p>
     *
     * @param lhs the left operand, must not be {@code null}
     * @param rhs the right operand, must not be {@code null}
     */
    record Impl(Expression lhs, Expression rhs) implements ModArithmeticExpr {
    }
}