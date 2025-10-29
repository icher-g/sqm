package io.sqm.core;

import io.sqm.core.internal.LimitOffsetImpl;

/**
 * LIMIT/OFFSET pair (or OFFSET/FETCH mapped to these two).
 */
public non-sealed interface LimitOffset extends Node {
    /**
     * Convenient factory methods
     */
    static LimitOffset limit(long limit) {
        return new LimitOffsetImpl(limit, null);
    }

    static LimitOffset offset(long offset) {
        return new LimitOffsetImpl(null, offset);
    }

    static LimitOffset of(Long limit, Long offset) {
        return new LimitOffsetImpl(limit, offset);
    }

    Long limit();   // null if absent

    Long offset();  // null if absent
}
