package io.sqm.core.match;

import io.sqm.core.GroupItem;

import java.util.function.Function;

/**
 * Default implementation of {@link GroupItemMatch}.
 *
 * @param <R> the result type produced by the match
 */
public class GroupItemMatchImpl<R> implements GroupItemMatch<R> {

    private final GroupItem item;
    private boolean matched = false;
    private R result;

    public GroupItemMatchImpl(GroupItem item) {
        this.item = item;
    }

    @Override
    public GroupItemMatch<R> simple(Function<GroupItem.SimpleGroupItem, R> f) {
        if (!matched && item instanceof GroupItem.SimpleGroupItem i) {
            result = f.apply(i);
            matched = true;
        }
        return this;
    }

    @Override
    public GroupItemMatch<R> groupingSets(Function<GroupItem.GroupingSets, R> f) {
        if (!matched && item instanceof GroupItem.GroupingSets i) {
            result = f.apply(i);
            matched = true;
        }
        return this;
    }

    @Override
    public GroupItemMatch<R> groupingSet(Function<GroupItem.GroupingSet, R> f) {
        if (!matched && item instanceof GroupItem.GroupingSet i) {
            result = f.apply(i);
            matched = true;
        }
        return this;
    }

    @Override
    public GroupItemMatch<R> rollup(Function<GroupItem.Rollup, R> f) {
        if (!matched && item instanceof GroupItem.Rollup i) {
            result = f.apply(i);
            matched = true;
        }
        return this;
    }

    @Override
    public GroupItemMatch<R> cube(Function<GroupItem.Cube, R> f) {
        if (!matched && item instanceof GroupItem.Cube i) {
            result = f.apply(i);
            matched = true;
        }
        return this;
    }

    @Override
    public R otherwise(Function<GroupItem, R> f) {
        return matched ? result : f.apply(item);
    }
}
