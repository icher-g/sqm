package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Represent a '*' in SELECT. {@code SELECT *}.
 */
public non-sealed interface StarSelectItem extends SelectItem {

    /**
     * Creates a '*' placeholder in a SELECT statement.
     *
     * @return {@link StarSelectItem}.
     */
    static StarSelectItem of() {
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
        return v.visitStarSelectItem(this);
    }

    /**
     * Implements '*' in SELECT * statement.
     */
    record Impl() implements StarSelectItem {
    }
}
