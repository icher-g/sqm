package io.cherlabs.sqm.render.spi;

/**
 * An interface to hold all dialect related staff.
 */
public interface SqlDialect {
    /**
     * The name of the dialect. Mostly for the debugging/logging purposes.
     *
     * @return a name of the dialect.
     */
    String name();

    /**
     * Gets an identifier quoter that decides how identifiers are quoted/qualified.
     *
     * @return a quoter.
     */
    IdentifierQuoter quoter();

    /**
     * Gets a value formatter that formats a value into a string.
     *
     * @return a formatter.
     */
    ValueFormatter formatter();

    /**
     * Gets parameters placeholder that defines the placeholder style.
     *
     * @return a placeholder.
     */
    Placeholders placeholders();

    /**
     * Gets operators that customise tokens for arithmetic/comparison/string ops.
     *
     * @return operators.
     */
    Operators operators();

    /**
     * Gets booleans that define boolean literals & predicate rules.
     *
     * @return booleans.
     */
    Booleans booleans();

    /**
     * Gets null sorting definition that provides null ordering policy and emulation.
     *
     * @return null sorting.
     */
    NullSorting nullSorting();

    /**
     * Gets a pagination style definition.
     *
     * @return a pagination style.
     */
    PaginationStyle paginationStyle();

    /**
     * Gets renderers repository.
     *
     * @return a repository.
     */
    RenderersRepository renderers();
}
