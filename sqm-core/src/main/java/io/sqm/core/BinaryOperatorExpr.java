package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * A binary operator expression of the form:
 * <pre>{@code
 * <left> <operator> <right>
 * }</pre>
 *
 * <p>This node models SQL operators that are not represented as function calls,
 * which is especially important for PostgreSQL where many features are expressed via operators
 * (JSON/JSONB, regex, arrays, ranges, custom operators).</p>
 *
 * <p>The operator is stored as structured {@link OperatorName} metadata so dialect-specific syntax
 * such as PostgreSQL {@code OPERATOR(schema.op)} can be preserved and rendered deterministically.
 * The operator is exposed as {@link OperatorName} to preserve dialect-specific structure.</p>
 *
 * <p>Examples (PostgreSQL)</p>
 * <ul>
 *   <li>{@code payload -> 'user'} becomes {@code BinaryOperatorExpr.of(payload, "->", 'user')}</li>
 *   <li>{@code payload ->> 'id'} becomes {@code BinaryOperatorExpr.of(payload, "->>", 'id')}</li>
 *   <li>{@code data @> '{"a":1}'} becomes {@code BinaryOperatorExpr.of(data, "@>", '{"a":1}')}</li>
 *   <li>{@code name ~* 'abc'} becomes {@code BinaryOperatorExpr.of(name, "~*", 'abc')}</li>
 *   <li>{@code tags && other_tags} becomes {@code BinaryOperatorExpr.of(tags, "&&", otherTags)}</li>
 * </ul>
 */
public non-sealed interface BinaryOperatorExpr extends Expression {

    /**
     * Creates a binary operator expression.
     *
     * @param left     left operand
     * @param operator operator token (for example {@code "->"} or {@code "@>"})
     * @param right    right operand
     * @return binary operator expression
     */
    static BinaryOperatorExpr of(Expression left, String operator, Expression right) {
        return of(left, OperatorName.of(operator), right);
    }

    /**
     * Creates a binary operator expression with a structured operator name.
     *
     * @param left     left operand
     * @param operator structured operator name
     * @param right    right operand
     * @return binary operator expression
     */
    static BinaryOperatorExpr of(Expression left, OperatorName operator, Expression right) {
        return new Impl(left, operator, right);
    }

    /**
     * Left operand of the operator.
     *
     * @return left operand expression
     */
    Expression left();

    /**
     * Structured operator metadata.
     *
     * @return operator metadata
     */
    OperatorName operator();

    /**
     * Right operand of the operator.
     *
     * @return right operand expression
     */
    Expression right();

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
        return v.visitBinaryOperatorExpr(this);
    }

    /**
     * Default implementation.
     * <p>
     * Nested to keep the model change self-contained. You may later move it to your standard
     * implementation package without changing the public API.
     *
     * @param left     left operand
     * @param operator structured operator metadata
     * @param right    right operand
     */
    record Impl(Expression left, OperatorName operator, Expression right) implements BinaryOperatorExpr {

        /**
         * Creates a binary operator expression implementation.
         *
         * @param left     left operand
         * @param operator structured operator metadata
         * @param right    right operand
         */
        public Impl {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(operator, "operator");
            Objects.requireNonNull(right, "right");
        }
    }
}
