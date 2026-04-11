package io.sqm.codegen;

/**
 * Represents a 1-based line and column pair used for source diagnostics.
 *
 * @param line line number
 * @param column column number
 */
record LineColumn(int line, int column) {
}
