package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Represents a bit string literal (e.g., {@code B'1010'}).
 */
public sealed interface BitStringLiteralExpr extends LiteralExpr permits BitStringLiteralExpr.Impl {
    /**
     * Creates a bit string literal expression.
     *
     * @param value literal value without surrounding quotes
     * @return a new {@link BitStringLiteralExpr}
     */
    static BitStringLiteralExpr of(String value) {
        return new Impl(value);
    }

    /**
     * Returns the literal value without surrounding quotes.
     *
     * @return literal value
     */
    @Override
    String value();

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
        return v.visitBitStringLiteralExpr(this);
    }

    /**
     * Default implementation for {@link BitStringLiteralExpr}.
     *
     * @param value literal value without surrounding quotes
     */
    record Impl(String value) implements BitStringLiteralExpr {
        /**
         * Creates a {@link BitStringLiteralExpr.Impl} instance.
         *
         * @param value literal value without surrounding quotes
         */
        public Impl {
            Objects.requireNonNull(value, "value");
        }
    }
}
