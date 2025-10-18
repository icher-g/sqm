package io.cherlabs.sqm.core;

/**
 * LIMIT/OFFSET as a value object. Nulls mean “not set”.
 */
public record LimitOffset(Long limit, Long offset) implements Entity {

    public LimitOffset {
        if (limit != null && limit < 0) {
            throw new IllegalArgumentException("limit must be >= 0");
        }
        if (offset != null && offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0");
        }
        // normalize 0-length strings / etc. not needed; just keep nulls or >=0
    }

    /**
     * Convenient factory methods
     */
    public static LimitOffset limit(long limit) {
        return new LimitOffset(limit, null);
    }

    public static LimitOffset offset(long offset) {
        return new LimitOffset(null, offset);
    }

    public static LimitOffset of(Long limit, Long offset) {
        return new LimitOffset(limit, offset);
    }

    public boolean hasLimit() {
        return limit != null;
    }

    public boolean hasOffset() {
        return offset != null;
    }
}
