package io.sqm.codegen;

import java.util.Locale;
import java.util.Objects;

/**
 * Supported dialects for SQL file code generation validation.
 */
public enum SqlCodegenDialect {
    ANSI,
    POSTGRESQL;

    /**
     * Resolves a dialect from user configuration.
     *
     * @param value dialect name such as {@code ansi} or {@code postgresql}.
     * @return resolved dialect.
     */
    public static SqlCodegenDialect from(String value) {
        Objects.requireNonNull(value, "value");
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "ansi" -> ANSI;
            case "postgresql", "postgres", "pg" -> POSTGRESQL;
            default -> throw new IllegalArgumentException("Unsupported dialect: " + value);
        };
    }
}
