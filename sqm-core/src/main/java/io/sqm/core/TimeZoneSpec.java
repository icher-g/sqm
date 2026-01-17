package io.sqm.core;

/**
 * Models the optional PostgreSQL time zone clause for {@code TIME} / {@code TIMESTAMP} types.
 *
 * <p>Examples:</p>
 * <ul>
 *   <li>{@code timestamp} -> {@link #NONE}</li>
 *   <li>{@code timestamp with time zone} -> {@link #WITH_TIME_ZONE}</li>
 *   <li>{@code timestamp without time zone} -> {@link #WITHOUT_TIME_ZONE}</li>
 * </ul>
 */
public enum TimeZoneSpec {

    /**
     * No time zone clause is present (or not applicable).
     */
    NONE,

    /**
     * {@code WITH TIME ZONE}.
     */
    WITH_TIME_ZONE,

    /**
     * {@code WITHOUT TIME ZONE}.
     */
    WITHOUT_TIME_ZONE
}
