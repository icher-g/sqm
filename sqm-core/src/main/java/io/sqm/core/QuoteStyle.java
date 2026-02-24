package io.sqm.core;

/**
 * Describes the quote delimiter style used for an identifier in the source SQL.
 */
public enum QuoteStyle {
    /**
     * Unquoted identifier.
     */
    NONE,
    /**
     * ANSI / PostgreSQL style double quotes: {@code "name"}.
     */
    DOUBLE_QUOTE,
    /**
     * MySQL style backticks: {@code `name`}.
     */
    BACKTICK,
    /**
     * SQL Server style brackets: {@code [name]}.
     */
    BRACKETS
}

