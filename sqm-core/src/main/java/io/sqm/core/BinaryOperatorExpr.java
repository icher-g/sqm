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
 * <p>The operator is stored as a raw {@link String} and is rendered as-is by a dialect renderer.
 * Dialect-specific validation (for example, allow-listing PostgreSQL operators such as {@code ->}, {@code @>}, {@code ~*})
 * should be implemented in a PostgreSQL module, not in the core model.</p>
 *
 * <h3>Examples (PostgreSQL)</h3>
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
        return new Impl(left, operator, right);
    }

    /**
     * Left operand of the operator.
     *
     * @return left operand expression
     */
    Expression left();

    /**
     * Operator token to apply between operands.
     * <p>
     * Stored as raw text and interpreted by the dialect renderer and validator.
     *
     * @return operator token, not blank
     */
    String operator();

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
     */
    record Impl(Expression left, String operator, Expression right) implements BinaryOperatorExpr {

        public Impl {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(operator, "operator");
            Objects.requireNonNull(right, "right");
            if (operator.isBlank()) {
                throw new IllegalArgumentException("operator must not be blank");
            }
        }
    }
}
