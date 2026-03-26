package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Identifier-valued hint argument.
 */
public non-sealed interface IdentifierHintArg extends HintArg {

    /**
     * Creates an identifier hint argument with the provided value.
     *
     * @param value an identifier.
     * @return a new instance of identifier hint argument.
     */
    static IdentifierHintArg of(Identifier value) {
        return new Impl(value);
    }

    /**
     * Gets an identifier value.
     *
     * @return identifier value.
     */
    Identifier value();

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
        return v.visitIdentifierHintArg(this);
    }

    /**
     * Identifier-valued hint argument.
     *
     * @param value identifier value.
     */
    record Impl(Identifier value) implements IdentifierHintArg {
        /**
         * Creates an identifier hint argument.
         *
         * @param value identifier value
         */
        public Impl {
            Objects.requireNonNull(value, "value");
        }
    }
}