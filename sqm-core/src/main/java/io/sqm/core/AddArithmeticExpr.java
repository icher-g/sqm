package io.sqm.core;

import io.sqm.core.internal.AddArithmeticExprImpl;
import io.sqm.core.match.ArithmeticMatch;
import io.sqm.core.match.Match;
import io.sqm.core.walk.NodeVisitor;

/**
 * Represents an addition operation of the form {@code lhs + rhs}.
 */
public non-sealed interface AddArithmeticExpr extends AdditiveArithmeticExpr {

    /**
     * Creates a new addition expression.
     *
     * @param lhs the left operand, must not be {@code null}
     * @param rhs the right operand, must not be {@code null}
     * @return a new {@code AddArithmeticExpr} instance
     */
    static AddArithmeticExpr of(Expression lhs, Expression rhs) {
        return new AddArithmeticExprImpl(lhs, rhs);
    }

    /**
     * Creates a new matcher for the current {@link ArithmeticExpr}.
     *
     * @param <R> the result type
     * @return a new {@code ArithmeticMatch}.
     */
    default <R> ArithmeticMatch<R> matchArithmetic() {
        return Match.arithmetic(this);
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
        return v.visitAddArithmeticExpr(this);
    }
}

