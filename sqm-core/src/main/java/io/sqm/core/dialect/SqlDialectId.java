package io.sqm.core.dialect;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

/**
 * Stable identifier for a SQL dialect family.
 *
 * @param value normalized dialect identifier
 */
public record SqlDialectId(String value) implements Serializable {
    /**
     * Built-in ANSI dialect identifier.
     */
    public static final SqlDialectId ANSI = new SqlDialectId("ansi");

    /**
     * Built-in MySQL dialect identifier.
     */
    public static final SqlDialectId MYSQL = new SqlDialectId("mysql");

    /**
     * Built-in PostgreSQL dialect identifier.
     */
    public static final SqlDialectId POSTGRESQL = new SqlDialectId("postgresql");

    /**
     * Built-in SQL Server dialect identifier.
     */
    public static final SqlDialectId SQLSERVER = new SqlDialectId("sqlserver");

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
        return switch (normalized) {
            case "ansi" -> ANSI;
            case "mysql" -> MYSQL;
            case "postgres", "postgresql" -> POSTGRESQL;
            case "sqlserver", "mssql", "tsql" -> SQLSERVER;
            default -> new SqlDialectId(normalized);
        };
    }
}
