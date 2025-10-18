package io.sqm.render.ansi.spi;

import io.sqm.render.spi.PaginationStyle;

public class AnsiPaginationStyle implements PaginationStyle {

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
