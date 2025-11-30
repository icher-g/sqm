package io.sqm.core;

import io.sqm.core.internal.MulArithmeticExprImpl;
import io.sqm.core.walk.NodeVisitor;

/**
 * Represents a multiplication operation of the form {@code lhs * rhs}.
 */
public non-sealed interface MulArithmeticExpr extends MultiplicativeArithmeticExpr {

    /**
     * Creates a new multiplication expression.
     *
     * @param lhs the left operand, must not be {@code null}
     * @param rhs the right operand, must not be {@code null}
     * @return a new {@code MulArithmeticExpr} instance
     */
    static MulArithmeticExpr of(Expression lhs, Expression rhs) {
        return new MulArithmeticExprImpl(lhs, rhs);
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
        return v.visitMulArithmeticExpr(this);
    }
}

