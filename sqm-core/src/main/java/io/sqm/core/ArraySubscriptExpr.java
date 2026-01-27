package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Represents array subscripting (indexing) of an expression.
 *
 * <p>This node models expressions such as:</p>
 * <ul>
 *   <li>{@code arr[1]}</li>
 *   <li>{@code arr[1][2]} (modeled as nested {@code ArraySubscriptExpr} nodes)</li>
 *   <li>{@code ARRAY[1,2,3][2]}</li>
 * </ul>
 *
 * <p>This is an expression-level feature and must not be confused with array type syntax
 * such as {@code int[]} which is represented by {@link TypeName#arrayDims()}.</p>
 *
 * <p>Semantics are dialect-specific. For example, PostgreSQL arrays are 1-based. This node
 * does not encode indexing rules and only captures SQL structure.</p>
 */
public non-sealed interface ArraySubscriptExpr extends Expression {

    /**
     * Creates an array subscript expression.
     *
     * @param base  the expression being indexed
     * @param index the index expression inside brackets
     * @return a new {@link ArraySubscriptExpr}
     * @throws NullPointerException if {@code base} or {@code index} is {@code null}
     */
    static ArraySubscriptExpr of(Expression base, Expression index) {
        return new Impl(base, index);
    }

    /**
     * Returns the expression being indexed.
     *
     * <p>For {@code arr[1]} this is {@code arr}. For chained subscripts such as
     * {@code arr[1][2]}, this will itself be an {@link ArraySubscriptExpr}.</p>
     *
     * @return the base expression
     */
    Expression base();

    /**
     * Returns the index expression inside brackets.
     *
     * <p>For {@code arr[1]} this is the literal {@code 1}. The index can be any expression,
     * for example {@code arr[i + 1]}.</p>
     *
     * @return the index expression
     */
    Expression index();

    /**
     * Accepts a {@link NodeVisitor}.
     *
     * <p>To support this node, {@link NodeVisitor} should provide a
     * {@code visitArraySubscriptExpr(ArraySubscriptExpr)} method.</p>
     *
     * @param visitor the visitor
     * @param <R> visitor return type
     * @return the visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitArraySubscriptExpr(this);
    }

    /**
     * Default immutable implementation of {@link ArraySubscriptExpr}.
     *
     * @param base the expression being indexed
     * @param index the index expression
     */
    record Impl(Expression base, Expression index) implements ArraySubscriptExpr {
        public Impl {
            Objects.requireNonNull(base, "base");
            Objects.requireNonNull(index, "index");
        }
    }
}
