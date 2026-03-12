package io.sqm.core.dialect;

import java.util.Locale;
import java.util.Objects;

/**
 * Stable identifier for a SQL dialect family.
 *
 * @param value normalized dialect identifier
 */
public record SqlDialectId(String value) {
    /**
     * Creates a normalized dialect identifier.
     *
     * @param value normalized dialect identifier
     */
    public SqlDialectId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    /**
     * Creates a dialect identifier from raw input.
     *
     * @param value raw dialect identifier
     * @return normalized dialect identifier
     */
    public static SqlDialectId of(String value) {
        Objects.requireNonNull(value, "value");
        var normalized = value.trim().toLowerCase(Locale.ROOT);
        return new SqlDialectId(switch (normalized) {
            case "postgres" -> "postgresql";
            default -> normalized;
        });
    }
}
