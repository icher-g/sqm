package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;
import io.sqm.core.match.GroupItemMatch;

import java.util.List;
import java.util.Objects;

/**
 * Represents a single element in a {@code GROUP BY} clause.
 * <p>
 * Group items can be simple expressions or ordinals, or represent
 * grouping extensions such as {@code GROUPING SETS}, {@code ROLLUP},
 * or {@code CUBE}.
 */
public non-sealed interface GroupItem extends Node {
    /**
     * Creates a group by item from column.
     *
     * @param expr a column to group by
     * @return A newly created instance of a group item.
     */
    static SimpleGroupItem of(Expression expr) {
        return new SimpleGroupItemImpl(Objects.requireNonNull(expr), null);
    }

    /**
     * Creates a group by item from ordinal.
     *
     * @param ordinal an ordinal to group by.
     * @return A newly created instance of a group item.
     */
    static SimpleGroupItem of(int ordinal) {
        return new SimpleGroupItemImpl(null, ordinal);
    }

    /**
     * Creates a grouping set element, for example {@code (a, b)} or {@code ()}.
     *
     * @param items grouping items inside the set
     * @return a grouping set item
     */
    static GroupItem groupingSet(List<GroupItem> items) {
        return new GroupingSetImpl(items);
    }

    /**
     * Creates a grouping set element, for example {@code (a, b)} or {@code ()}.
     *
     * @param items grouping items inside the set
     * @return a grouping set item
     */
    static GroupItem groupingSet(GroupItem... items) {
        return new GroupingSetImpl(List.of(items));
    }

    /**
     * Creates a {@code GROUPING SETS (...)} element.
     *
     * @param sets grouping set elements
     * @return a grouping sets item
     */
    static GroupItem groupingSets(List<GroupItem> sets) {
        return new GroupingSetsImpl(sets);
    }

    /**
     * Creates a {@code GROUPING SETS (...)} element.
     *
     * @param sets grouping set elements
     * @return a grouping sets item
     */
    static GroupItem groupingSets(GroupItem... sets) {
        return new GroupingSetsImpl(List.of(sets));
    }

    /**
     * Creates a {@code ROLLUP (...)} element.
     *
     * @param items grouping items inside the rollup
     * @return a rollup item
     */
    static GroupItem rollup(List<GroupItem> items) {
        return new RollupImpl(items);
    }

    /**
     * Creates a {@code ROLLUP (...)} element.
     *
     * @param items grouping items inside the rollup
     * @return a rollup item
     */
    static GroupItem rollup(GroupItem... items) {
        return new RollupImpl(List.of(items));
    }

    /**
     * Creates a {@code CUBE (...)} element.
     *
     * @param items grouping items inside the cube
     * @return a cube item
     */
    static GroupItem cube(List<GroupItem> items) {
        return new CubeImpl(items);
    }

    /**
     * Creates a {@code CUBE (...)} element.
     *
     * @param items grouping items inside the cube
     * @return a cube item
     */
    static GroupItem cube(GroupItem... items) {
        return new CubeImpl(List.of(items));
    }

    /**
     * Creates a new matcher for the current {@link GroupItem}.
     *
     * @param <R> the result type produced by the match
     * @return a new {@link GroupItemMatch} for this grouping item
     */
    default <R> GroupItemMatch<R> matchGroupItem() {
        return GroupItemMatch.match(this);
    }

    /**
     * A grouping item representing a parenthesized grouping set.
     * <p>Examples:</p>
     * <pre>
     *     {@code
     *     GROUP BY (a, b)
     *     GROUP BY ()
     *     }
     * </pre>
     */
    sealed interface GroupingSet extends GroupItem permits GroupingSetImpl {
        /**
         * Gets items inside the grouping set.
         *
         * @return items inside the grouping set
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
            return v.visitGroupingSet(this);
        }
    }

    /**
     * A grouping item representing {@code GROUPING SETS (...)}.
     */
    sealed interface GroupingSets extends GroupItem permits GroupingSetsImpl {
        /**
         * Gets grouping set elements.
         *
         * @return grouping set elements
         */
        List<GroupItem> sets();

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
            return v.visitGroupingSets(this);
        }
    }

    /**
     * A grouping item representing {@code ROLLUP (...)}.
     */
    sealed interface Rollup extends GroupItem permits RollupImpl {
        /**
         * Gets items inside the rollup.
         *
         * @return items inside the rollup
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
            return v.visitRollup(this);
        }
    }

    /**
     * A grouping item representing {@code CUBE (...)}.
     */
    sealed interface Cube extends GroupItem permits CubeImpl {
        /**
         * Gets items inside the cube.
         *
         * @return items inside the cube
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
            return v.visitCube(this);
        }
    }

    /**
     * A simple grouping item representing either an expression or ordinal.
     */
    sealed interface SimpleGroupItem extends GroupItem permits SimpleGroupItemImpl {
        /**
         * Gets a group item expression.
         *
         * @return an expression if used or {@code null} otherwise.
         */
        Expression expr();

        /**
         * 1-based; may be null if expression is used.
         *
         * @return an ordinal if used or {@code null} otherwise.
         */
        Integer ordinal();

        /**
         * Indicates if the {@link GroupItem} is represented by ordinal.
         *
         * @return True if the group is represented by ordinal or False otherwise.
         */
        default boolean isOrdinal() {
            return ordinal() != null;
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
            return v.visitSimpleGroupItem(this);
        }
    }

    /**
     * Default implementation for {@link SimpleGroupItem}.
     *
     * @param expr    an expression.
     * @param ordinal an ordinal.
     */
    record SimpleGroupItemImpl(Expression expr, Integer ordinal) implements SimpleGroupItem {
    }

    /**
     * Default implementation for {@link GroupingSet}.
     *
     * @param items grouping items inside the set
     */
    record GroupingSetImpl(List<GroupItem> items) implements GroupingSet {
        /**
         * Creates a grouping set implementation.
         *
         * @param items grouping items inside the set
         */
        public GroupingSetImpl {
            items = List.copyOf(items);
        }
    }

    /**
     * Default implementation for {@link GroupingSets}.
     *
     * @param sets grouping set elements
     */
    record GroupingSetsImpl(List<GroupItem> sets) implements GroupingSets {
        /**
         * Creates a grouping sets implementation.
         *
         * @param sets grouping set elements
         */
        public GroupingSetsImpl {
            sets = List.copyOf(sets);
        }
    }

    /**
     * Default implementation for {@link Rollup}.
     *
     * @param items grouping items inside the rollup
     */
    record RollupImpl(List<GroupItem> items) implements Rollup {
        /**
         * Creates a rollup implementation.
         *
         * @param items grouping items inside the rollup
         */
        public RollupImpl {
            items = List.copyOf(items);
        }
    }

    /**
     * Default implementation for {@link Cube}.
     *
     * @param items grouping items inside the cube
     */
    record CubeImpl(List<GroupItem> items) implements Cube {
        /**
         * Creates a cube implementation.
         *
         * @param items grouping items inside the cube
         */
        public CubeImpl {
            items = List.copyOf(items);
        }
    }
}
