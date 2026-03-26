package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Expression-valued hint argument.
 */
public non-sealed interface ExpressionHintArg extends HintArg {

    /**
     * Creates an expression-valued hint argument.
     *
     * @param value expression value
     * @return immutable expression hint argument
     */
    static ExpressionHintArg of(Expression value) {
        return new Impl(value);
    }

    /**
     * Gets an expression value.
     *
     * @return expression value
     */
    Expression value();

    /**
     * Accepts a {@link NodeVisitor} that performs an operation on this node.
     * <p>
     * Each concrete node class calls back into the visitor with a type-specific
     * {@code visitXxx(...)} method, allowing the visitor to handle each node
     * type appropriately.
     * </p>
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type produced by the visitor
     * @return the result of the visitor's operation on this node,
     * or {@code null} if the visitor's return type is {@link Void}
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitExpressionHintArg(this);
    }

    /**
     * Expression-valued hint argument.
     *
     * @param value expression value
     */
    record Impl(Expression value) implements ExpressionHintArg {
        /**
         * Creates an expression hint argument.
         */
        public Impl {
            Objects.requireNonNull(value, "value");
        }
    }
}
