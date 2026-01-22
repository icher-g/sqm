package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Collections;
import java.util.List;

/**
 * Represents a GroupBy statement with the list of Group items.
 */
public non-sealed interface GroupBy extends Node {

    /**
     * Creates GROUP BY statement.
     *
     * @param items a list of group by items.
     * @return a new GROUP BY statement.
     */
    static GroupBy of(List<GroupItem> items) {
        return new Impl(items);
    }

    /**
     * Gets a list of group items.
     *
     * @return a list of group items.
     */
    List<GroupItem> items();

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
        return v.visitGroupBy(this);
    }

    /**
     * Implements a GroupBy statement with the list of Group items.
     *
     * @param items a list of group by items.
     */
    record Impl(List<GroupItem> items) implements GroupBy {
        /**
         * Ensures items are unmodifiable.
         *
         * @param items a list of items.
         */
        public Impl {
            items = Collections.unmodifiableList(items);
        }
    }
}
