package io.sqm.playground.api;

/**
 * Request payload for SQL parsing in the playground.
 *
 * @param sql SQL text to parse
 * @param dialect source SQL dialect
 */
public record ParseRequestDto(String sql, SqlDialectDto dialect) {
}
