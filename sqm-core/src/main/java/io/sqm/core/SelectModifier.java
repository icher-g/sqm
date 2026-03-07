package io.sqm.core;

/**
 * Represents a non-standard SELECT-level modifier token.
 * <p>
 * Modifiers are rendered between {@code SELECT} and the projection list.
 */
public enum SelectModifier {
    /**
     * MySQL {@code SQL_CALC_FOUND_ROWS} modifier.
     */
    CALC_FOUND_ROWS
}
