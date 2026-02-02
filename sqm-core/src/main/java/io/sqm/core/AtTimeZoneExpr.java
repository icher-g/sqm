package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Represents a PostgreSQL {@code AT TIME ZONE} expression for timezone conversion.
 * <p>
 * This expression converts a timestamp to a different time zone. The operand is typically
 * a timestamp (with or without time zone), and the timezone argument can be a string literal,
 * an expression/column, or an interval expression.
 * <p>
 * Examples:
 * <ul>
 *   <li>{@code timestamp_column AT TIME ZONE 'UTC'}</li>
 *   <li>{@code current_timestamp AT TIME ZONE 'America/New_York'}</li>
 *   <li>{@code '2024-01-01 12:00:00'::timestamp AT TIME ZONE interval '-05:00'}</li>
 * </ul>
 */
public non-sealed interface AtTimeZoneExpr extends Expression {

    /**
     * Creates an AT TIME ZONE expression.
     *
     * @param timestamp the timestamp expression to convert
     * @param timezone  the target time zone (string, column, or interval expression)
     * @return AT TIME ZONE expression
     */
    static AtTimeZoneExpr of(Expression timestamp, Expression timezone) {
        return new Impl(timestamp, timezone);
    }

    /**
     * The timestamp expression being converted.
     *
     * @return timestamp expression operand
     */
    Expression timestamp();

    /**
     * The target time zone expression.
     * <p>
     * This can be a string literal (e.g., {@code 'UTC'}), a column reference,
     * or an interval expression.
     *
     * @return timezone expression, not null
     */
    Expression timezone();

    /**
     * Accepts a {@link NodeVisitor} and dispatches control to the
     * visitor method corresponding to the concrete subtype.
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type returned by the visitor
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitAtTimeZoneExpr(this);
    }

    /**
     * Default implementation.
     * <p>
     * Nested to keep the model change self-contained.
     *
     * @param timestamp the timestamp expression to convert
     * @param timezone  the target time zone expression
     */
    record Impl(Expression timestamp, Expression timezone) implements AtTimeZoneExpr {

        /**
         * Creates an AT TIME ZONE expression implementation.
         *
         * @param timestamp the timestamp expression to convert
         * @param timezone  the target time zone expression
         */
        public Impl {
            Objects.requireNonNull(timestamp, "timestamp");
            Objects.requireNonNull(timezone, "timezone");
        }
    }
}
