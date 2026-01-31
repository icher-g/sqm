package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Represents a PostgreSQL escape string literal (e.g., {@code E'...'}).
 */
public sealed interface EscapeStringLiteralExpr extends LiteralExpr permits EscapeStringLiteralExpr.Impl {
    /**
     * Creates an escape string literal expression.
     *
     * @param value literal value without surrounding quotes
     * @return a new {@link EscapeStringLiteralExpr}
     */
    static EscapeStringLiteralExpr of(String value) {
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
        return v.visitEscapeStringLiteralExpr(this);
    }

    /**
     * Default implementation for {@link EscapeStringLiteralExpr}.
     *
     * @param value literal value without surrounding quotes
     */
    record Impl(String value) implements EscapeStringLiteralExpr {
        /**
         * Creates a {@link EscapeStringLiteralExpr.Impl} instance.
         *
         * @param value literal value without surrounding quotes
         */
        public Impl {
            Objects.requireNonNull(value, "value");
        }
    }
}
