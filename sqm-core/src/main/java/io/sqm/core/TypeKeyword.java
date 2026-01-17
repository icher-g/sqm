package io.sqm.core;

/**
 * Represents SQL-standard multi-word type keywords.
 *
 * <p>This enum is intentionally closed and represents only
 * grammar-defined type spellings, not user-defined identifiers.</p>
 */
public enum TypeKeyword {

    DOUBLE_PRECISION("double precision"),
    CHARACTER_VARYING("character varying"),
    NATIONAL_CHARACTER("national character"),
    NATIONAL_CHARACTER_VARYING("national character varying");

    private final String sql;

    TypeKeyword(String sql) {
        this.sql = sql;
    }

    /**
     * Returns the canonical SQL rendering of this keyword type.
     */
    public String sql() {
        return sql;
    }
}

