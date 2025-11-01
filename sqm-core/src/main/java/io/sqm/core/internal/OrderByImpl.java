package io.sqm.core.internal;

import io.sqm.core.OrderBy;
import io.sqm.core.OrderItem;

import java.util.List;

/**
 * Implements an OrderBy statement with the list of Order items.
 *
 * @param items a list of order by items.
 */
public record OrderByImpl(List<OrderItem> items) implements OrderBy {

    public OrderByImpl {
        items = List.copyOf(items);
    }
}
