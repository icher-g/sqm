package io.sqm.render.oracle.spi;

import io.sqm.render.spi.PaginationStyle;

/**
 * Oracle pagination style definition.
 */
public class OraclePaginationStyle implements PaginationStyle {
    /**
     * Creates Oracle pagination-style definition.
     */
    public OraclePaginationStyle() {
    }

    @Override
    public boolean supportsLimitOffset() {
        return false;
    }

    @Override
    public boolean supportsOffsetFetch() {
        return true;
    }

    @Override
    public boolean supportsTop() {
        return false;
    }
}
