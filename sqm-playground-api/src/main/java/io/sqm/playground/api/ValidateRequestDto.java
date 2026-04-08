package io.sqm.playground.api;

/**
 * Request payload for SQL validation in the playground.
 *
 * @param sql SQL text to validate
 * @param dialect SQL dialect to validate against
 */
public record ValidateRequestDto(String sql, SqlDialectDto dialect) {
}
