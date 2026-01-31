package io.sqm.core;

/**
 * Represents SQL-standard multi-word type keywords.
 *
 * <p>This enum is intentionally closed and represents only
 * grammar-defined type spellings, not user-defined identifiers.</p>
 */
public enum TypeKeyword {

    /** DOUBLE PRECISION type keyword. */
    DOUBLE_PRECISION("double precision"),
    /** CHARACTER VARYING type keyword. */
    CHARACTER_VARYING("character varying"),
    /** NATIONAL CHARACTER type keyword. */
    NATIONAL_CHARACTER("national character"),
    /** NATIONAL CHARACTER VARYING type keyword. */
    NATIONAL_CHARACTER_VARYING("national character varying");

    private final String sql;

    TypeKeyword(String sql) {
        this.sql = sql;
    }

    /**
     * Returns the canonical SQL rendering of this keyword type.
     *
     * @return SQL keyword string
     */
    public String sql() {
        return sql;
    }
}

