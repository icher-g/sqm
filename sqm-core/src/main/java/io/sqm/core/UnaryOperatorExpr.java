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
 * <p>The operator is stored as structured {@link OperatorName} metadata.
 * The operator is exposed as structured {@link OperatorName} metadata.</p>
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
        return of(OperatorName.of(operator), expr);
    }

    /**
     * Creates a unary operator expression with a structured operator name.
     *
     * @param operator structured operator metadata
     * @param expr operand expression
     * @return unary operator expression
     */
    static UnaryOperatorExpr of(OperatorName operator, Expression expr) {
        return new Impl(operator, expr);
    }

    /**
     * Structured operator metadata.
     *
     * @return operator metadata
     */
    OperatorName operator();

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
     * @param operator structured operator metadata
     * @param expr     operand expression
     */
    record Impl(OperatorName operator, Expression expr) implements UnaryOperatorExpr {

        /**
         * Creates a unary operator expression implementation.
         *
         * @param operator structured operator metadata
         * @param expr     operand expression
         */
        public Impl {
            Objects.requireNonNull(operator, "operator");
            Objects.requireNonNull(expr, "expr");
        }
    }
}
