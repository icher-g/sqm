package io.sqm.core.internal;

import io.sqm.core.LimitOffset;

public record LimitOffsetImpl(Long limit, Long offset) implements LimitOffset {

    public LimitOffsetImpl {
        if (limit != null && limit < 0) {
            throw new IllegalArgumentException("limit must be >= 0");
        }
        if (offset != null && offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0");
        }
        // normalize 0-length strings / etc. not needed; just keep nulls or >=0
    }

    public boolean hasLimit() {
        return limit != null;
    }

    public boolean hasOffset() {
        return offset != null;
    }
}
