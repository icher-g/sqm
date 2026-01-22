package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * A unary arithmetic expression that represents numeric negation.
 *
 * <p>This corresponds to the SQL syntax {@code -expr}. It changes the sign of
 * the underlying expression but does not introduce additional semantics.</p>
 *
 * <p>Unary plus is intentionally not modeled, since {@code +expr} has no
 * semantic effect and can be safely ignored by the parser.</p>
 */
public non-sealed interface NegativeArithmeticExpr extends ArithmeticExpr {

    /**
     * Creates a new negative arithmetic expression for the given operand.
     *
     * @param expr the expression to negate, must not be {@code null}
     * @return a new {@code NegativeArithmeticExpr} instance
     */
    static NegativeArithmeticExpr of(Expression expr) {
        return new Impl(expr);
    }

    /**
     * Returns the negated expression.
     *
     * @return the operand being negated, never {@code null}
     */
    Expression expr();

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
        return v.visitNegativeArithmeticExpr(this);
    }

    /**
     * A unary arithmetic expression that represents numeric negation.
     *
     * <p>This corresponds to the SQL syntax {@code -expr}. It changes the sign of
     * the underlying expression but does not introduce additional semantics.</p>
     *
     * <p>Unary plus is intentionally not modeled, since {@code +expr} has no
     * semantic effect and can be safely ignored by the parser.</p>
     *
     * @param expr the expression to negate, must not be {@code null}
     */
    record Impl(Expression expr) implements NegativeArithmeticExpr {
    }
}

