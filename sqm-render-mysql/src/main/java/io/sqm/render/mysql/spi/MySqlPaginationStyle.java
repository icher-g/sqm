package io.sqm.render.mysql.spi;

import io.sqm.render.spi.PaginationStyle;

/**
 * MySQL pagination style definition.
 */
public final class MySqlPaginationStyle implements PaginationStyle {

    /**
     * Creates MySQL pagination style definition.
     */
    public MySqlPaginationStyle() {
    }

    /**
     * Indicates MySQL supports LIMIT/OFFSET syntax.
     *
     * @return true.
     */
    @Override
    public boolean supportsLimitOffset() {
        return true;
    }

    /**
     * Indicates MySQL does not use OFFSET/FETCH syntax.
     *
     * @return false.
     */
    @Override
    public boolean supportsOffsetFetch() {
        return false;
    }

    /**
     * Indicates MySQL does not use TOP syntax.
     *
     * @return false.
     */
    @Override
    public boolean supportsTop() {
        return false;
    }
}
