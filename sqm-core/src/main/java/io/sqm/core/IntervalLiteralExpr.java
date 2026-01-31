package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents an {@code INTERVAL '...'} literal.
 */
public sealed interface IntervalLiteralExpr extends LiteralExpr permits IntervalLiteralExpr.Impl {
    /**
     * Creates an interval literal expression without a qualifier.
     *
     * @param value literal value without surrounding quotes
     * @return a new {@link IntervalLiteralExpr}
     */
    static IntervalLiteralExpr of(String value) {
        return new Impl(value, Optional.empty());
    }

    /**
     * Creates an interval literal expression with a qualifier.
     *
     * @param value     literal value without surrounding quotes
     * @param qualifier interval qualifier (e.g., {@code DAY} or {@code DAY TO SECOND})
     * @return a new {@link IntervalLiteralExpr}
     */
    static IntervalLiteralExpr of(String value, String qualifier) {
        return new Impl(value, Optional.ofNullable(qualifier));
    }

    /**
     * Returns the literal value without surrounding quotes.
     *
     * @return literal value
     */
    @Override
    String value();

    /**
     * Returns the optional interval qualifier.
     *
     * @return optional interval qualifier
     */
    Optional<String> qualifier();

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
        return v.visitIntervalLiteralExpr(this);
    }

    /**
     * Default implementation for {@link IntervalLiteralExpr}.
     *
     * @param value     literal value without surrounding quotes
     * @param qualifier optional qualifier
     */
    record Impl(String value, Optional<String> qualifier) implements IntervalLiteralExpr {
        /**
         * Creates a {@link IntervalLiteralExpr.Impl} instance.
         *
         * @param value     literal value without surrounding quotes
         * @param qualifier optional qualifier
         */
        public Impl {
            Objects.requireNonNull(value, "value");
            Objects.requireNonNull(qualifier, "qualifier");
        }
    }
}
