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
    MYSQL,

    /**
     * SQL Server dialect.
     */
    SQLSERVER,

    /**
     * Oracle dialect.
     */
    ORACLE;

    /**
     * Resolves a dialect from user configuration.
     *
     * @param value dialect name such as {@code ansi}, {@code postgresql}, {@code mysql}, {@code sqlserver}, or {@code oracle}.
     * @return resolved dialect.
     */
    public static SqlCodegenDialect from(String value) {
        Objects.requireNonNull(value, "value");
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "ansi" -> ANSI;
            case "postgresql", "postgres", "pg" -> POSTGRESQL;
            case "mysql" -> MYSQL;
            case "sqlserver", "mssql", "tsql" -> SQLSERVER;
            case "oracle", "ora" -> ORACLE;
            default -> throw new IllegalArgumentException("Unsupported dialect: " + value);
        };
    }
}
