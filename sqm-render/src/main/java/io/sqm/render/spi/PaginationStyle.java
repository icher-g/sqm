package io.sqm.render.spi;

/**
 * An interface to describe the way the pagination is supported by the dialect.
 */
public interface PaginationStyle {
    /**
     * Indicates if the dialect supports limit and offset.
     *
     * @return True if supported and False otherwise.
     */
    boolean supportsLimitOffset();

    /**
     * Indicates if the dialect supports offset fetch.
     *
     * @return True if supported and False otherwise.
     */
    boolean supportsOffsetFetch();

    /**
     * Indicates if the dialect supports top.
     *
     * @return True if supported and False otherwise.
     */
    boolean supportsTop();
}
