package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Qualified-name-valued hint argument.
 */
public non-sealed interface QualifiedNameHintArg extends HintArg {

    /**
     * Creates a qualified-name hint argument.
     *
     * @param qualifiedName a qualified name.
     * @return a new instance of qualified-name hint argument.
     */
    static QualifiedNameHintArg of(QualifiedName qualifiedName) {
        return new Impl(qualifiedName);
    }

    /**
     * Gets qualified name value.
     *
     * @return qualified name value.
     */
    QualifiedName value();

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
        return v.visitQualifiedNameHintArg(this);
    }

    /**
     * Qualified-name-valued hint argument.
     *
     * @param value qualified name value
     */
    record Impl(QualifiedName value) implements QualifiedNameHintArg {
        /**
         * Creates a qualified-name hint argument.
         *
         * @param value qualified name value
         */
        public Impl {
            Objects.requireNonNull(value, "value");
        }
    }
}