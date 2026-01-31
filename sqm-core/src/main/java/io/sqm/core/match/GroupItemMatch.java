package io.sqm.core.match;

import io.sqm.core.GroupItem;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link GroupItem} subtypes.
 *
 * @param <R> the result type produced by the match
 */
public interface GroupItemMatch<R> extends Match<GroupItem, R> {

    /**
     * Creates a new matcher for the given {@link GroupItem}.
     *
     * @param i   the group item to match on
     * @param <R> the result type
     * @return a new {@code GroupItemMatch} for {@code i}
     */
    static <R> GroupItemMatch<R> match(GroupItem i) {
        return new GroupItemMatchImpl<>(i);
    }

    /**
     * Registers a handler for simple group items (expr/ordinal).
     *
     * @param f handler for simple group items
     * @return {@code this} for fluent chaining
     */
    GroupItemMatch<R> simple(Function<GroupItem.SimpleGroupItem, R> f);

    /**
     * Registers a handler for {@code GROUPING SETS (...)}.
     *
     * @param f handler for grouping sets
     * @return {@code this} for fluent chaining
     */
    GroupItemMatch<R> groupingSets(Function<GroupItem.GroupingSets, R> f);

    /**
     * Registers a handler for a single grouping set {@code (...)}.
     *
     * @param f handler for grouping sets
     * @return {@code this} for fluent chaining
     */
    GroupItemMatch<R> groupingSet(Function<GroupItem.GroupingSet, R> f);

    /**
     * Registers a handler for {@code ROLLUP (...)}.
     *
     * @param f handler for rollup
     * @return {@code this} for fluent chaining
     */
    GroupItemMatch<R> rollup(Function<GroupItem.Rollup, R> f);

    /**
     * Registers a handler for {@code CUBE (...)}.
     *
     * @param f handler for cube
     * @return {@code this} for fluent chaining
     */
    GroupItemMatch<R> cube(Function<GroupItem.Cube, R> f);
}
