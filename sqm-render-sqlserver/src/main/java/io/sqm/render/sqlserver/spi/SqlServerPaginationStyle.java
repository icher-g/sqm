package io.sqm.render.sqlserver.spi;

import io.sqm.render.spi.PaginationStyle;

/**
 * SQL Server pagination style definition.
 */
public final class SqlServerPaginationStyle implements PaginationStyle {

    /**
     * Creates SQL Server pagination-style definition.
     */
    public SqlServerPaginationStyle() {
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
        return true;
    }
}
