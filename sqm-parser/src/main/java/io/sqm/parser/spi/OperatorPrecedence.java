package io.sqm.parser.spi;

/**
 * Represents precedence tiers for custom (non-built-in) SQL operators.
 * <p>
 * These tiers are used by dialect-specific parsers to decide how tightly
 * user-defined operators bind relative to each other, while keeping all
 * custom operators below arithmetic precedence and above predicate forms.
 * <p>
 * Higher tiers bind more tightly. Operators in the same tier are parsed as
 * left-associative.
 */
public enum OperatorPrecedence {

    /**
     * Lowest-precedence custom operators.
     */
    CUSTOM_LOW(1),

    /**
     * Middle-precedence custom operators.
     */
    CUSTOM_MEDIUM(2),

    /**
     * Highest-precedence custom operators.
     */
    CUSTOM_HIGH(3);

    private final int level;

    OperatorPrecedence(int level) {
        this.level = level;
    }

    /**
     * Returns the numeric precedence level (higher means tighter binding).
     *
     * @return precedence level
     */
    public int level() {
        return level;
    }
}