package io.sqm.playground.api;

/**
 * Stable SQL dialect identifiers used by the playground API.
 */
public enum SqlDialectDto {
    /**
     * ANSI dialect
     */
    ansi,
    /**
     * PostgreSQL dialect
     */
    postgresql,
    /**
     * MySQL dialect
     */
    mysql,
    /**
     * SQL Server dialect
     */
    sqlserver,
    /**
     * Oracle dialect
     */
    oracle;

    /**
     * Resolves a dialect from its lowercase API value.
     *
     * @param value lowercase API value
     * @return matching dialect
     */
    public static SqlDialectDto fromValue(String value) {
        try {
            return SqlDialectDto.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown SQL dialect: " + value, e);
        }
    }
}
