package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents array slicing of an expression.
 *
 * <p>This node models expressions such as:</p>
 * <ul>
 *   <li>{@code arr[2:5]}</li>
 *   <li>{@code arr[:5]} (missing lower bound)</li>
 *   <li>{@code arr[2:]} (missing upper bound)</li>
 *   <li>{@code arr[:]} (both bounds missing, dialect-dependent meaning)</li>
 * </ul>
 *
 * <p>This is an expression-level feature and must not be confused with array type syntax
 * such as {@code int[]} which is represented by {@link TypeName#arrayDims()}.</p>
 *
 * <p>The bounds are modeled as optional expressions. Dialect-specific semantics such as
 * whether bounds are inclusive, whether indexes are 0-based or 1-based, and how omitted
 * bounds behave are not encoded by this node and must be handled by the target dialect,
 * validator, or runtime.</p>
 */
public non-sealed interface ArraySliceExpr extends Expression {

    /**
     * Creates an array slice expression.
     *
     * @param base the expression being sliced
     * @param from the optional lower bound expression (may be {@code null} to indicate absence)
     * @param to   the optional upper bound expression (may be {@code null} to indicate absence)
     * @return a new {@link ArraySliceExpr}
     * @throws NullPointerException if {@code base} is {@code null}
     */
    static ArraySliceExpr of(Expression base, Expression from, Expression to) {
        return new Impl(base, Optional.ofNullable(from), Optional.ofNullable(to));
    }

    /**
     * Returns the expression being sliced.
     *
     * <p>For {@code arr[2:5]} this is {@code arr}.</p>
     *
     * @return the base expression
     */
    Expression base();

    /**
     * Returns the optional lower bound expression.
     *
     * <p>For {@code arr[2:5]} this is {@code 2}. For {@code arr[:5]} it is empty.</p>
     *
     * @return the optional lower bound expression
     */
    Optional<Expression> from();

    /**
     * Returns the optional upper bound expression.
     *
     * <p>For {@code arr[2:5]} this is {@code 5}. For {@code arr[2:]} it is empty.</p>
     *
     * @return the optional upper bound expression
     */
    Optional<Expression> to();

    /**
     * Accepts a {@link NodeVisitor}.
     *
     * <p>To support this node, {@link NodeVisitor} should provide a
     * {@code visitArraySliceExpr(ArraySliceExpr)} method.</p>
     *
     * @param visitor the visitor
     * @param <R>     visitor return type
     * @return the visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitArraySliceExpr(this);
    }

    /**
     * Default immutable implementation of {@link ArraySliceExpr}.
     *
     * @param base the expression being sliced
     * @param from optional lower bound
     * @param to   optional upper bound
     */
    record Impl(Expression base, Optional<Expression> from, Optional<Expression> to) implements ArraySliceExpr {
        public Impl {
            Objects.requireNonNull(base, "base");
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
        }
    }
}
