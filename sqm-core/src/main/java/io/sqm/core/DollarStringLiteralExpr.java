package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Represents a PostgreSQL dollar-quoted string literal (e.g., {@code $$...$$}).
 */
public sealed interface DollarStringLiteralExpr extends LiteralExpr permits DollarStringLiteralExpr.Impl {
    /**
     * Creates a dollar-quoted string literal expression.
     *
     * @param tag   dollar tag (empty for {@code $$...$$})
     * @param value literal value without delimiters
     * @return a new {@link DollarStringLiteralExpr}
     */
    static DollarStringLiteralExpr of(String tag, String value) {
        return new Impl(tag, value);
    }

    /**
     * Returns the dollar tag used in the literal.
     *
     * @return dollar tag, never {@code null} (may be empty)
     */
    String tag();

    /**
     * Returns the literal value without delimiters.
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
        return v.visitDollarStringLiteralExpr(this);
    }

    /**
     * Default implementation for {@link DollarStringLiteralExpr}.
     *
     * @param tag   dollar tag (empty for {@code $$...$$})
     * @param value literal value without delimiters
     */
    record Impl(String tag, String value) implements DollarStringLiteralExpr {
        /**
         * Creates a {@link DollarStringLiteralExpr.Impl} instance.
         *
         * @param tag   dollar tag (empty for {@code $$...$$})
         * @param value literal value without delimiters
         */
        public Impl {
            Objects.requireNonNull(tag, "tag");
            Objects.requireNonNull(value, "value");
        }
    }
}
