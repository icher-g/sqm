package io.cherlabs.sqlmodel.render.spi;

/**
 * An interface to customize boolean representation in a dialect.
 */
public interface Booleans {
    /**
     * Gets a TRUE literal for the dialect.
     * @return a TRUE literal.
     */
    String trueLiteral();

    /**
     * Gets a FALSE literal for the dialect.
     * @return a FALSE literal.
     */
    String falseLiteral();

    /**
     * Indicates whether an explicit predicate is required.
     * For example: {@code WHERE active = TRUE}
     * @return True if the predicate is required or False otherwise.
     */
    boolean requireExplicitPredicate();
}
