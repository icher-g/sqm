package io.sqm.core;

import io.sqm.core.internal.LimitOffsetImpl;
import io.sqm.core.walk.NodeVisitor;

/**
 * LIMIT/OFFSET pair (or OFFSET/FETCH mapped to these two).
 */
public non-sealed interface LimitOffset extends Node {
    /**
     * Initializes the class with provided limit.
     *
     * @param limit a limit to use.
     * @return new instance of {@link LimitOffset}.
     */
    static LimitOffset limit(long limit) {
        return new LimitOffsetImpl(limit, null);
    }

    /**
     * Creates a {@link LimitOffset} with the provided offset.
     *
     * @param offset an offset to use.
     * @return new instance of {@link LimitOffset}.
     */
    static LimitOffset offset(long offset) {
        return new LimitOffsetImpl(null, offset);
    }

    /**
     * Creates a {@link LimitOffset} with the provided limit and offset.
     *
     * @param limit  a limit to use.
     * @param offset an offset to use.
     * @return new instance of {@link LimitOffset}.
     */
    static LimitOffset of(Long limit, Long offset) {
        return new LimitOffsetImpl(limit, offset);
    }

    /**
     * Gets a limit. null if absent.
     *
     * @return a limit.
     */
    Long limit();

    /**
     * Gets an offset. null if absent.
     *
     * @return an offset.
     */
    Long offset();

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
