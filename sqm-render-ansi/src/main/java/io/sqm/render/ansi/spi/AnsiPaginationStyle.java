package io.sqm.render.ansi.spi;

import io.sqm.render.spi.PaginationStyle;

/**
 * ANSI pagination style definition.
 */
public class AnsiPaginationStyle implements PaginationStyle {

    /**
     * Creates ANSI pagination-style definition.
     */
    public AnsiPaginationStyle() {
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
