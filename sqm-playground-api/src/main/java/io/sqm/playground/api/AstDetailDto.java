package io.sqm.playground.api;

/**
 * Scalar metadata entry attached to an AST node.
 *
 * @param name detail name
 * @param value detail value
 */
public record AstDetailDto(String name, String value) {
}
