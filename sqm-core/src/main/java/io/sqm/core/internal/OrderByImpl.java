package io.sqm.core.internal;

import io.sqm.core.OrderBy;
import io.sqm.core.OrderItem;

import java.util.Collections;
import java.util.List;

/**
 * Implements an OrderBy statement with the list of Order items.
 *
 * @param items a list of order by items.
 */
public record OrderByImpl(List<OrderItem> items) implements OrderBy {
    /**
     * Ensures items are unmodifiable.
     *
     * @param items a list of items.
     */
    public OrderByImpl {
        items = Collections.unmodifiableList(items);
    }
}
