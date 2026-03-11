package io.sqm.codegen;

import java.util.Locale;
import java.util.Objects;

/**
 * Supported dialects for SQL file code generation validation.
 */
public enum SqlCodegenDialect {
    /**
     * ANSI SQL dialect.
     */
    ANSI,

    /**
     * PostgreSQL dialect.
     */
    POSTGRESQL,

    /**
     * MySQL dialect.
     */
    MYSQL;

    /**
     * Resolves a dialect from user configuration.
     *
     * @param value dialect name such as {@code ansi}, {@code postgresql}, or {@code mysql}.
     * @return resolved dialect.
     */
    public static SqlCodegenDialect from(String value) {
        Objects.requireNonNull(value, "value");
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "ansi" -> ANSI;
            case "postgresql", "postgres", "pg" -> POSTGRESQL;
            case "mysql" -> MYSQL;
            default -> throw new IllegalArgumentException("Unsupported dialect: " + value);
        };
    }
}
