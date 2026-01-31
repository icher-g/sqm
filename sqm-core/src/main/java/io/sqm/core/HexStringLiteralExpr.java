package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Represents a hex string literal (e.g., {@code X'FF'}).
 */
public sealed interface HexStringLiteralExpr extends LiteralExpr permits HexStringLiteralExpr.Impl {
    /**
     * Creates a hex string literal expression.
     *
     * @param value literal value without surrounding quotes
     * @return a new {@link HexStringLiteralExpr}
     */
    static HexStringLiteralExpr of(String value) {
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
        return v.visitHexStringLiteralExpr(this);
    }

    /**
     * Default implementation for {@link HexStringLiteralExpr}.
     *
     * @param value literal value without surrounding quotes
     */
    record Impl(String value) implements HexStringLiteralExpr {
        /**
         * Creates a {@link HexStringLiteralExpr.Impl} instance.
         *
         * @param value literal value without surrounding quotes
         */
        public Impl {
            Objects.requireNonNull(value, "value");
        }
    }
}
