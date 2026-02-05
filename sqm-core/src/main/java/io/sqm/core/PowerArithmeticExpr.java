package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Represents PostgreSQL exponentiation using the {@code ^} operator.
 * <p>
 * Precedence: higher than {@code * / %} and {@code + -}.
 * <p>
 * Associativity: left-to-right in PostgreSQL (e.g. {@code 2 ^ 3 ^ 3} is parsed as
 * {@code (2 ^ 3) ^ 3} unless parentheses are used).
 */
public non-sealed interface PowerArithmeticExpr extends ArithmeticExpr {

    /**
     * Creates an exponentiation expression {@code left ^ right}.
     *
     * @param lhs base expression
     * @param rhs exponent expression
     * @return a new {@link PowerArithmeticExpr}
     */
    static PowerArithmeticExpr of(Expression lhs, Expression rhs) {
        return new Impl(lhs, rhs);
    }

    /**
     * Base expression (left operand).
     *
     * @return left operand.
     */
    Expression lhs();

    /**
     * Exponent expression (right operand).
     *
     * @return right operand.
     */
    Expression rhs();

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
        return v.visitPowerArithmeticExpr(this);
    }

    /**
     * Represents a multiplication operation of the form {@code lhs * rhs}.
     *
     * @param lhs the left operand, must not be {@code null}
     * @param rhs the right operand, must not be {@code null}
     */
    record Impl(Expression lhs, Expression rhs) implements PowerArithmeticExpr {
    }
}

