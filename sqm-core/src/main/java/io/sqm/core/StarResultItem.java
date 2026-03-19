package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Represent a '*' in RETURNING. {@code RETURNING * }.
 */
public non-sealed interface StarResultItem extends ResultItem {

    /**
     * Creates a '*' placeholder in a RETURNING statement.
     *
     * @return {@link StarResultItem}.
     */
    static StarResultItem of() {
        return new Impl();
    }

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
        return v.visitStarResultItem(this);
    }

    /**
     * Implements '*' in RETURNING * statement.
     */
    record Impl() implements StarResultItem {
    }
}
