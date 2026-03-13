package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;

/**
 * A SELECT-style query.
 *
 * <p>This interface is immutable and exposes only read operations. Use
 * {@link SelectQueryBuilder} (for example via {@link #builder()}) to construct
 * or edit {@link SelectQuery} instances.</p>
 */
public non-sealed interface SelectQuery extends Query {

    /**
     * Creates an immutable {@link SelectQuery} from all SELECT clause parts.
     *
     * <p>This is a low-level factory intended for callers that already have the
     * full query shape available. For incremental construction use
     * {@link #builder()}.</p>
     *
     * @param items select items (must not be {@code null})
     * @param from FROM table reference, or {@code null}
     * @param joins joins (must not be {@code null})
     * @param where WHERE predicate, or {@code null}
     * @param groupBy GROUP BY clause, or {@code null}
     * @param having HAVING predicate, or {@code null}
     * @param orderBy ORDER BY clause, or {@code null}
     * @param distinct DISTINCT specification, or {@code null}
     * @param limitOffset LIMIT/OFFSET specification, or {@code null}
     * @param lockFor locking clause, or {@code null}
     * @param windows WINDOW clause definitions (must not be {@code null})
     * @return immutable {@link SelectQuery} instance
     */
    static SelectQuery of(
        List<SelectItem> items,
        TableRef from,
        List<Join> joins,
        Predicate where,
        GroupBy groupBy,
        Predicate having,
        OrderBy orderBy,
        DistinctSpec distinct,
        LimitOffset limitOffset,
        LockingClause lockFor,
        List<WindowDef> windows) {
        return new Impl(items, from, joins, where, groupBy, having, orderBy, distinct, limitOffset, lockFor, windows, List.of(), List.of());
    }

    /**
     * Creates an immutable {@link SelectQuery} from all SELECT clause parts, including select modifiers and optimizer hints.
     *
     * @param items select items (must not be {@code null})
     * @param from FROM table reference, or {@code null}
     * @param joins joins (must not be {@code null})
     * @param where WHERE predicate, or {@code null}
     * @param groupBy GROUP BY clause, or {@code null}
     * @param having HAVING predicate, or {@code null}
     * @param orderBy ORDER BY clause, or {@code null}
     * @param distinct DISTINCT specification, or {@code null}
     * @param limitOffset LIMIT/OFFSET specification, or {@code null}
     * @param lockFor locking clause, or {@code null}
     * @param windows WINDOW clause definitions (must not be {@code null})
     * @param modifiers select modifiers
     * @param optimizerHints optimizer hints (without comment delimiters)
     * @return immutable {@link SelectQuery} instance
     */
    static SelectQuery of(
        List<SelectItem> items,
        TableRef from,
        List<Join> joins,
        Predicate where,
        GroupBy groupBy,
        Predicate having,
        OrderBy orderBy,
        DistinctSpec distinct,
        LimitOffset limitOffset,
        LockingClause lockFor,
        List<WindowDef> windows,
        List<SelectModifier> modifiers,
        List<String> optimizerHints) {
        return new Impl(items, from, joins, where, groupBy, having, orderBy, distinct, limitOffset, lockFor, windows, modifiers, optimizerHints);
    }

    /**
     * Creates a mutable builder for constructing an immutable {@link SelectQuery}.
     *
     * @return new select-query builder
     */
    static SelectQueryBuilder builder() {
        return SelectQueryBuilder.of();
    }

    /**
     * Creates a mutable builder initialized from an existing {@link SelectQuery}.
     *
     * @param query source query
     * @return builder initialized with the query state
     */
    static SelectQueryBuilder builder(SelectQuery query) {
        return SelectQueryBuilder.of(query);
    }

    /**
     * Gets a list of select items to be used in the SELECT clause.
     *
     * @return immutable list of select items
     */
    List<SelectItem> items();

    /**
     * Gets a table reference used in the FROM clause.
     *
     * @return table reference or {@code null}
     */
    TableRef from();

    /**
     * Gets a list of joins.
     *
     * @return immutable list of joins
     */
    List<Join> joins();

    /**
     * Gets a predicate used in the WHERE clause.
     *
     * @return predicate or {@code null}
     */
    Predicate where();

    /**
     * Gets the GROUP BY clause.
     *
     * @return group-by clause or {@code null}
     */
    GroupBy groupBy();

    /**
     * Gets a predicate used in the HAVING clause.
     *
     * @return predicate or {@code null}
     */
    Predicate having();

    /**
     * Gets window definitions declared in the WINDOW clause.
     *
     * @return immutable list of window definitions
     */
    List<WindowDef> windows();

    /**
     * Gets the ORDER BY clause.
     *
     * @return order-by clause or {@code null}
     */
    OrderBy orderBy();

    /**
     * Returns the DISTINCT specification of this SELECT query.
     *
     * @return DISTINCT specification, or {@code null} if not present
     */
    DistinctSpec distinct();

    /**
     * Gets the limit/offset specification for this query.
     *
     * @return limit/offset specification or {@code null} if absent
     */
    LimitOffset limitOffset();

    /**
     * Returns the locking clause associated with this SELECT query.
     *
     * @return locking clause or {@code null}
     */
    LockingClause lockFor();

    /**
     * Returns select-level modifiers.
     *
     * @return immutable list of select modifiers.
     */
    default List<SelectModifier> modifiers() {
        return List.of();
    }

    /**
     * Returns optimizer hints attached to this query.
     * <p>
     * Values do not include comment delimiters and are rendered as {@code /*+ ... *\/} by supporting renderers.
     *
     * @return immutable list of optimizer hints.
     */
    default List<String> optimizerHints() {
        return List.of();
    }

    /**
     * Accepts a {@link NodeVisitor} and dispatches control to the
     * visitor method corresponding to the concrete subtype.
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type returned by the visitor
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitSelectQuery(this);
    }

    /**
     * Default immutable implementation of {@link SelectQuery}.
     *
     * @param items select items (immutable copy)
     * @param from FROM table reference, or {@code null}
     * @param joins joins (immutable copy)
     * @param where WHERE predicate, or {@code null}
     * @param groupBy GROUP BY clause, or {@code null}
     * @param having HAVING predicate, or {@code null}
     * @param orderBy ORDER BY clause, or {@code null}
     * @param distinct DISTINCT specification, or {@code null}
     * @param limitOffset LIMIT/OFFSET specification, or {@code null}
     * @param lockFor locking clause, or {@code null}
     * @param windows WINDOW clause definitions (immutable copy)
     * @param modifiers select modifiers (immutable copy)
     * @param optimizerHints optimizer hints (immutable copy)
     */
    record Impl(List<SelectItem> items,
                TableRef from,
                List<Join> joins,
                Predicate where,
                GroupBy groupBy,
                Predicate having,
                OrderBy orderBy,
                DistinctSpec distinct,
                LimitOffset limitOffset,
                LockingClause lockFor,
                List<WindowDef> windows,
                List<SelectModifier> modifiers,
                List<String> optimizerHints) implements SelectQuery {

        /**
         * Creates an immutable {@link SelectQuery} implementation and defensively copies list inputs.
         */
        public Impl {
            items = List.copyOf(items);
            joins = List.copyOf(joins);
            windows = List.copyOf(windows);
            modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
            optimizerHints = optimizerHints == null ? List.of() : List.copyOf(optimizerHints);
        }

        /**
         * Creates an immutable {@link SelectQuery} implementation without select modifiers and optimizer hints.
         *
         * @param items select items (must not be {@code null})
         * @param from FROM table reference, or {@code null}
         * @param joins joins (must not be {@code null})
         * @param where WHERE predicate, or {@code null}
         * @param groupBy GROUP BY clause, or {@code null}
         * @param having HAVING predicate, or {@code null}
         * @param orderBy ORDER BY clause, or {@code null}
         * @param distinct DISTINCT specification, or {@code null}
         * @param limitOffset LIMIT/OFFSET specification, or {@code null}
         * @param lockFor locking clause, or {@code null}
         * @param windows WINDOW clause definitions (must not be {@code null})
         */
        public Impl(List<SelectItem> items,
                    TableRef from,
                    List<Join> joins,
                    Predicate where,
                    GroupBy groupBy,
                    Predicate having,
                    OrderBy orderBy,
                    DistinctSpec distinct,
                    LimitOffset limitOffset,
                    LockingClause lockFor,
                    List<WindowDef> windows) {
            this(items, from, joins, where, groupBy, having, orderBy, distinct, limitOffset, lockFor, windows, List.of(), List.of());
        }
    }
}

