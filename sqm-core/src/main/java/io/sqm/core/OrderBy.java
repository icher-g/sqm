package io.sqm.core;

import io.sqm.core.internal.OrderByImpl;

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
    static OrderBy of(List<OrderItem> items) {
        return new OrderByImpl(items);
    }

    /**
     * A list of OrderBy items.
     *
     * @return a list of order by items.
     */
    List<OrderItem> items();
}
