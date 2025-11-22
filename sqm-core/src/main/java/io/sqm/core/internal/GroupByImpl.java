package io.sqm.core.internal;

import io.sqm.core.GroupBy;
import io.sqm.core.GroupItem;

import java.util.Collections;
import java.util.List;

/**
 * Implements a GroupBy statement with the list of Group items.
 *
 * @param items a list of group by items.
 */
public record GroupByImpl(List<GroupItem> items) implements GroupBy {
    /**
     * Ensures items are unmodifiable.
     *
     * @param items a list of items.
     */
    public GroupByImpl {
        items = Collections.unmodifiableList(items);
    }
}
