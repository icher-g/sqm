package io.sqm.render.postgresql.spi;

import io.sqm.render.spi.PaginationStyle;

/**
 * PostgreSQL pagination style definition.
 */
public final class PostgresPaginationStyle implements PaginationStyle {
    /**
     * Creates PostgreSQL pagination-style definition.
     */
    public PostgresPaginationStyle() {
    }

    @Override
    public boolean supportsLimitOffset() {
        return true;
    }

    @Override
    public boolean supportsOffsetFetch() {
        return false;
    }

    @Override
    public boolean supportsTop() {
        return false;
    }
}

