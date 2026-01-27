package io.sqm.render.pgsql.spi;

import io.sqm.render.spi.PaginationStyle;

public final class PostgresPaginationStyle implements PaginationStyle {
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

