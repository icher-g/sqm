package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * A unary operator expression of the form:
 * <pre>{@code
 * <operator><expr>
 * }</pre>
 *
 * <p>This node models SQL unary operators that are represented using operator syntax.
 * In PostgreSQL, this can include arithmetic signs and other unary operators such as bitwise NOT ({@code ~}).</p>
 *
 * <p>The operator is stored as a raw {@link String} and is rendered as-is by a dialect renderer.
 * Dialect-specific validation should be implemented in the PostgreSQL module.</p>
 *
 * <p>Examples (PostgreSQL)</p>
 * <ul>
 *   <li>{@code -amount} becomes {@code UnaryOperatorExpr.of("-", amount)}</li>
 *   <li>{@code ~mask} becomes {@code UnaryOperatorExpr.of("~", mask)}</li>
 * </ul>
 */
public non-sealed interface UnaryOperatorExpr extends Expression {

    /**
     * Creates a unary operator expression.
     *
     * @param operator operator token (for example {@code "-"} or {@code "~"})
     * @param expr     operand expression
     * @return unary operator expression
     */
    static UnaryOperatorExpr of(String operator, Expression expr) {
        return new Impl(operator, expr);
    }

    /**
     * Operator token to apply to the operand.
     *
     * @return operator token, not blank
     */
    String operator();

    /**
     * Operand expression the operator is applied to.
     *
     * @return operand expression
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
        return v.visitUnaryOperatorExpr(this);
    }

    /**
     * Default implementation.
     * <p>
     * Nested to keep the model change self-contained. You may later move it to your standard
     * implementation package without changing the public API.
     *
     * @param operator operator token (for example {@code "-"} or {@code "~"})
     * @param expr     operand expression
     */
    record Impl(String operator, Expression expr) implements UnaryOperatorExpr {

        /**
         * Creates a unary operator expression implementation.
         *
         * @param operator operator token
         * @param expr     operand expression
         */
        public Impl {
            Objects.requireNonNull(operator, "operator");
            Objects.requireNonNull(expr, "expr");
            if (operator.isBlank()) {
                throw new IllegalArgumentException("operator must not be blank");
            }
        }
    }
}
