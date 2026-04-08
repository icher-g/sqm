package io.sqm.playground.api;

/**
 * Request payload for SQL transpilation in the playground.
 *
 * @param sql input SQL text
 * @param sourceDialect source SQL dialect
 * @param targetDialect target SQL dialect
 */
public record TranspileRequestDto(
    String sql,
    SqlDialectDto sourceDialect,
    SqlDialectDto targetDialect
) {
}
