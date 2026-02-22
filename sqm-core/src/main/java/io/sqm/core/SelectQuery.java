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
        return new Impl(items, from, joins, where, groupBy, having, orderBy, distinct, limitOffset, lockFor, windows);
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
                List<WindowDef> windows) implements SelectQuery {

        public Impl {
            items = List.copyOf(items);
            joins = List.copyOf(joins);
            windows = List.copyOf(windows);
        }
    }
}
