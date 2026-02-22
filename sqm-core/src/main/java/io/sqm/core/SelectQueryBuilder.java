package io.sqm.core;

import io.sqm.core.internal.SelectQueryBuilderImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable builder for constructing immutable {@link SelectQuery} instances efficiently.
 *
 * <p>The builder is intended for internal construction-heavy flows such as parsers and transformers,
 * while the resulting {@link SelectQuery} remains immutable.</p>
 */
public interface SelectQueryBuilder {
    /**
     * Creates an empty builder.
     *
     * @return new builder instance
     */
    static SelectQueryBuilder of() {
        return new SelectQueryBuilderImpl();
    }

    /**
     * Creates a builder initialized from an existing SELECT query snapshot.
     *
     * @param query source query
     * @return builder initialized with the query state
     */
    static SelectQueryBuilder of(SelectQuery query) {
        return new SelectQueryBuilderImpl(query);
    }

    private static LimitOffset limitOffsetInternal(Expression limit, Expression offset) {
        return LimitOffset.of(limit, offset);
    }

    /**
     * Adds SELECT items from expressions, select items, or subqueries.
     *
     * @param nodes nodes accepted in the SELECT clause
     * @return this builder
     */
    default SelectQueryBuilder select(Node... nodes) {
        var items = new ArrayList<SelectItem>();
        for (var expr : nodes) {
            switch (expr) {
                case Expression expression -> items.add(expression.toSelectItem());
                case SelectItem selectItem -> items.add(selectItem);
                case Query query -> items.add(Expression.subquery(query).toSelectItem());
                default -> throw new IllegalStateException("The provided node is not supported in the SELECT clause: " + expr);
            }
        }
        return select(items);
    }

    /**
     * Adds select items.
     *
     * @param items select items to append
     * @return this builder
     */
    SelectQueryBuilder select(List<SelectItem> items);

    /**
     * Sets the {@code FROM} clause.
     *
     * @param tableRef table reference, may be {@code null}
     * @return this builder
     */
    SelectQueryBuilder from(TableRef tableRef);

    /**
     * Adds joins.
     *
     * @param joins joins to append
     * @return this builder
     */
    SelectQueryBuilder join(List<Join> joins);

    /**
     * Adds joins.
     *
     * @param joins joins to append
     * @return this builder
     */
    default SelectQueryBuilder join(Join... joins) {
        return join(List.of(joins));
    }

    /**
     * Adds a single join.
     *
     * @param join join to append
     * @return this builder
     */
    SelectQueryBuilder join(Join join);

    /**
     * Sets the {@code WHERE} clause predicate.
     *
     * @param predicate predicate, may be {@code null}
     * @return this builder
     */
    SelectQueryBuilder where(Predicate predicate);

    /**
     * Sets the {@code GROUP BY} clause from items.
     *
     * @param items group-by items
     * @return this builder
     */
    SelectQueryBuilder groupBy(List<GroupItem> items);

    /**
     * Sets the {@code GROUP BY} clause from items.
     *
     * @param items group-by items
     * @return this builder
     */
    default SelectQueryBuilder groupBy(GroupItem... items) {
        return groupBy(List.of(items));
    }

    /**
     * Sets the {@code HAVING} clause predicate.
     *
     * @param predicate predicate, may be {@code null}
     * @return this builder
     */
    SelectQueryBuilder having(Predicate predicate);

    /**
     * Adds window definitions.
     *
     * @param windows window definitions to append
     * @return this builder
     */
    SelectQueryBuilder window(List<WindowDef> windows);

    /**
     * Adds window definitions.
     *
     * @param windows window definitions to append
     * @return this builder
     */
    default SelectQueryBuilder window(WindowDef... windows) {
        return window(List.of(windows));
    }

    /**
     * Adds a single window definition.
     *
     * @param window window definition to append
     * @return this builder
     */
    SelectQueryBuilder window(WindowDef window);

    /**
     * Sets the {@code ORDER BY} clause from items.
     *
     * @param items order-by items
     * @return this builder
     */
    SelectQueryBuilder orderBy(List<OrderItem> items);

    /**
     * Sets the {@code ORDER BY} clause from items.
     *
     * @param items order-by items
     * @return this builder
     */
    default SelectQueryBuilder orderBy(OrderItem... items) {
        return orderBy(List.of(items));
    }

    /**
     * Sets the distinct specification.
     *
     * @param distinctSpec distinct specification, may be {@code null}
     * @return this builder
     */
    SelectQueryBuilder distinct(DistinctSpec distinctSpec);

    /**
     * Sets {@code DISTINCT ON (...)} using the provided expressions.
     *
     * @param items distinct-on expressions
     * @return this builder
     */
    default SelectQueryBuilder distinct(List<Expression> items) {
        return distinct(DistinctSpec.on(items));
    }

    /**
     * Sets {@code DISTINCT ON (...)} using the provided expressions.
     *
     * @param items distinct-on expressions
     * @return this builder
     */
    default SelectQueryBuilder distinct(Expression... items) {
        return distinct(List.of(items));
    }

    /**
     * Sets the limit/offset clause.
     *
     * @param limitOffset limit/offset clause, may be {@code null}
     * @return this builder
     */
    SelectQueryBuilder limitOffset(LimitOffset limitOffset);

    /**
     * Sets the query limit value.
     *
     * @param limit limit value
     * @return this builder
     */
    default SelectQueryBuilder limit(long limit) {
        return limit(Expression.literal(limit));
    }

    /**
     * Sets the query limit expression.
     *
     * @param limit limit expression
     * @return this builder
     */
    default SelectQueryBuilder limit(Expression limit) {
        return limitOffset(limitOffsetInternal(limit, currentOffset()));
    }

    /**
     * Sets the query offset value.
     *
     * @param offset offset value
     * @return this builder
     */
    default SelectQueryBuilder offset(long offset) {
        return offset(Expression.literal(offset));
    }

    /**
     * Sets the query offset expression.
     *
     * @param offset offset expression
     * @return this builder
     */
    default SelectQueryBuilder offset(Expression offset) {
        return limitOffset(limitOffsetInternal(currentLimit(), offset));
    }

    /**
     * Sets the locking clause.
     *
     * @param lockingClause locking clause, may be {@code null}
     * @return this builder
     */
    SelectQueryBuilder lockFor(LockingClause lockingClause);

    /**
     * Sets the locking clause from explicit parameters.
     *
     * @param mode       lock mode
     * @param ofTables   lock target tables
     * @param nowait     whether NOWAIT is specified
     * @param skipLocked whether SKIP LOCKED is specified
     * @return this builder
     */
    default SelectQueryBuilder lockFor(LockMode mode, List<LockTarget> ofTables, boolean nowait, boolean skipLocked) {
        return lockFor(LockingClause.of(mode, ofTables, nowait, skipLocked));
    }

    /**
     * Builds an immutable {@link SelectQuery}.
     *
     * @return immutable select query
     */
    SelectQuery build();

    /**
     * Returns current limit/offset state held by the builder, if any.
     *
     * @return current limit/offset clause or {@code null}
     */
    LimitOffset currentLimitOffset();

    private Expression currentLimit() {
        LimitOffset limitOffset = currentLimitOffset();
        return limitOffset == null ? null : limitOffset.limit();
    }

    private Expression currentOffset() {
        LimitOffset limitOffset = currentLimitOffset();
        return limitOffset == null ? null : limitOffset.offset();
    }
}
