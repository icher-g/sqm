package io.sqm.playground.api;

/**
 * Request payload for SQL rendering in the playground.
 *
 * @param sql input SQL text
 * @param sourceDialect source SQL dialect
 * @param targetDialect render target SQL dialect
 */
public record RenderRequestDto(
    String sql,
    SqlDialectDto sourceDialect,
    SqlDialectDto targetDialect
) {
}
