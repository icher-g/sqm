package io.sqm.core.internal;

import io.sqm.core.LimitOffset;

/**
 * LIMIT/OFFSET pair (or OFFSET/FETCH mapped to these two).
 *
 * @param limit  a limit.
 * @param offset an offset.
 */
public record LimitOffsetImpl(Long limit, Long offset) implements LimitOffset {

    /**
     * This constructor validates limit and offset are >= 0.
     *
     * @param limit  a limit.
     * @param offset an offset.
     */
    public LimitOffsetImpl {
        if (limit != null && limit < 0) {
            throw new IllegalArgumentException("limit must be >= 0");
        }
        if (offset != null && offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0");
        }
    }
}
