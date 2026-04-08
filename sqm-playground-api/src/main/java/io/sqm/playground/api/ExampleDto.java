package io.sqm.playground.api;

/**
 * Built-in SQL example exposed by the playground.
 *
 * @param id stable example identifier
 * @param title display title
 * @param dialect example source dialect
 * @param sql SQL text shown to the user
 */
public record ExampleDto(
    String id,
    String title,
    SqlDialectDto dialect,
    String sql
) {
}
