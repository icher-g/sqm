package io.sqm.core;

import io.sqm.core.internal.GroupByImpl;

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
        return new GroupByImpl(items);
    }

    /**
     * Gets a list of group items.
     *
     * @return a list of group items.
     */
    List<GroupItem> items();
}
