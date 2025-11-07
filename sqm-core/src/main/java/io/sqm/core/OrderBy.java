package io.sqm.core;

import io.sqm.core.internal.OrderByImpl;
import io.sqm.core.walk.NodeVisitor;

import java.util.List;

/**
 * Represents an OrderBy statement with the list of Order items.
 */
public non-sealed interface OrderBy extends Node {

    /**
     * Creates ORDER BY statement.
     *
     * @param items a list of order by items.
     * @return a new instance of ORDER BY statement.
     */
    static OrderBy of(OrderItem... items) {
        return new OrderByImpl(List.of(items));
    }

    /**
     * Creates ORDER BY statement.
     *
     * @param items a list of order by items.
     * @return a new instance of ORDER BY statement.
     */
    static OrderBy of(List<OrderItem> items) {
        return new OrderByImpl(items);
    }

    /**
     * A list of OrderBy items.
     *
     * @return a list of order by items.
     */
    List<OrderItem> items();

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
        return v.visitOrderBy(this);
    }
}
