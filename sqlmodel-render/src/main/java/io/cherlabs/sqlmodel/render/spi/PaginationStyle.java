package io.cherlabs.sqlmodel.render.spi;

public interface PaginationStyle {
    boolean supportsLimitOffset();
    boolean supportsOffsetFetch();
    boolean supportsTop();
}
