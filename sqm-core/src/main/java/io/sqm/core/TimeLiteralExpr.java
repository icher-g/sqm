package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Represents a {@code TIME '...'} typed literal.
 */
public sealed interface TimeLiteralExpr extends LiteralExpr permits TimeLiteralExpr.Impl {
    /**
     * Creates a time literal expression without a time zone clause.
     *
     * @param value literal value without surrounding quotes
     * @return a new {@link TimeLiteralExpr}
     */
    static TimeLiteralExpr of(String value) {
        return new Impl(value, TimeZoneSpec.NONE);
    }

    /**
     * Creates a time literal expression with the provided time zone spec.
     *
     * @param value        literal value without surrounding quotes
     * @param timeZoneSpec time zone clause
     * @return a new {@link TimeLiteralExpr}
     */
    static TimeLiteralExpr of(String value, TimeZoneSpec timeZoneSpec) {
        return new Impl(value, timeZoneSpec);
    }

    /**
     * Returns the literal value without surrounding quotes.
     *
     * @return literal value
     */
    @Override
    String value();

    /**
     * Returns the time zone spec for this literal.
     *
     * @return time zone spec, never {@code null}
     */
    TimeZoneSpec timeZoneSpec();

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
        return v.visitTimeLiteralExpr(this);
    }

    /**
     * Default implementation for {@link TimeLiteralExpr}.
     *
     * @param value        literal value without surrounding quotes
     * @param timeZoneSpec time zone clause
     */
    record Impl(String value, TimeZoneSpec timeZoneSpec) implements TimeLiteralExpr {
        /**
         * Creates a {@link TimeLiteralExpr.Impl} instance.
         *
         * @param value        literal value without surrounding quotes
         * @param timeZoneSpec time zone clause
         */
        public Impl {
            Objects.requireNonNull(value, "value");
            timeZoneSpec = timeZoneSpec == null ? TimeZoneSpec.NONE : timeZoneSpec;
        }
    }
}
