package io.sqm.core;

/**
 * Dialect-aware nulls ordering preference.
 */
public enum Nulls {
    /**
     * Default, no nulls.
     */
    DEFAULT,
    /**
     * Nulls first.
     */
    FIRST,
    /**
     * Nulls last.
     */
    LAST
}
