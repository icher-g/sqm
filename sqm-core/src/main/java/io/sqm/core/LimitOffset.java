package io.sqm.core;

import io.sqm.core.internal.LimitOffsetImpl;
import io.sqm.core.walk.NodeVisitor;

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

    /**
     * Accepts a {@link NodeVisitor} and dispatches control to the
     * visitor method corresponding to the concrete subtype
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type returned by the visitor
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitLimitOffset(this);
    }
}
