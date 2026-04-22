package io.sqm.core;

import io.sqm.core.internal.SelectQueryBuilderImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
     * Sets group items from the provided objects. Only {@code String, Number and Expression} are supported.
     *
     * @param items a list of items.
     * @return this builder
     */
    default SelectQueryBuilder groupBy(Object... items) {
        var groupItems = Arrays.stream(items).map(GroupItem::from).toList();
        return groupBy(groupItems);
    }

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
     * Sets order items from the provided objects.
     *
     * <p>Supported inputs are existing order items, column-name strings,
     * numeric ordinals, and expressions.</p>
     *
     * @param items a list of items
     * @return this builder
     */
    default SelectQueryBuilder orderBy(Object... items) {
        var orderItems = Arrays.stream(items).map(OrderItem::from).toList();
        return orderBy(orderItems);
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
     * Sets the distinct specification.
     *
     * @param distinctSpec distinct specification, may be {@code null}
     * @return this builder
     */
    SelectQueryBuilder distinct(DistinctSpec distinctSpec);

    /**
     * Sets the top specification.
     *
     * @param topSpec top specification, may be {@code null}
     * @return this builder
     */
    SelectQueryBuilder top(TopSpec topSpec);

    /**
     * Sets a plain {@code TOP (<count>)} specification using a numeric value.
     *
     * @param count top count
     * @return this builder
     */
    default SelectQueryBuilder top(long count) {
        return top(Expression.literal(count));
    }

    /**
     * Sets a plain {@code TOP (<count>)} specification using an expression.
     *
     * @param count top count expression
     * @return this builder
     */
    default SelectQueryBuilder top(Expression count) {
        return top(TopSpec.of(count));
    }

    /**
     * Sets a plain {@code TOP (<count>)} specification using an expression.
     *
     * @param count    top count expression
     * @param percent  whether {@code PERCENT} is present
     * @param withTies whether {@code WITH TIES} is present
     * @return this builder
     */
    default SelectQueryBuilder top(Expression count, boolean percent, boolean withTies) {
        return top(TopSpec.of(count, percent, withTies));
    }

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
     * Appends select-level modifiers.
     *
     * @param modifiers modifiers to append
     * @return this builder
     */
    SelectQueryBuilder selectModifiers(List<SelectModifier> modifiers);

    /**
     * Appends a select-level modifier.
     *
     * @param modifier modifier to append
     * @return this builder
     */
    default SelectQueryBuilder selectModifier(SelectModifier modifier) {
        Objects.requireNonNull(modifier, "modifier must not be null");
        return selectModifiers(List.of(modifier));
    }

    /**
     * Appends typed statement hints.
     *
     * @param hints typed statement hints to append
     * @return this builder
     */
    SelectQueryBuilder hints(List<StatementHint> hints);

    /**
     * Appends a typed statement hint.
     *
     * @param hint typed statement hint
     * @return this builder
     */
    default SelectQueryBuilder hint(StatementHint hint) {
        Objects.requireNonNull(hint, "hint must not be null");
        return hints(List.of(hint));
    }

    /**
     * Appends a typed statement hint using convenience arguments.
     *
     * @param name hint name
     * @param args convenience hint arguments
     * @return this builder
     */
    default SelectQueryBuilder hint(String name, Object... args) {
        return hint(StatementHint.of(name, args));
    }

    /**
     * Clears typed statement hints currently held by the builder.
     *
     * @return this builder
     */
    SelectQueryBuilder clearHints();

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

    /**
     * Returns the current top specification held by the builder, if any.
     *
     * @return current top specification or {@code null}
     */
    TopSpec currentTopSpec();

    /**
     * Returns the current distinct specification held by the builder, if any.
     *
     * @return current distinct specification or {@code null}
     */
    DistinctSpec currentDistinct();

    /**
     * Returns the current order-by clause held by the builder, if any.
     *
     * @return current order-by clause or {@code null}
     */
    OrderBy currentOrderBy();

    private Expression currentLimit() {
        LimitOffset limitOffset = currentLimitOffset();
        return limitOffset == null ? null : limitOffset.limit();
    }

    private Expression currentOffset() {
        LimitOffset limitOffset = currentLimitOffset();
        return limitOffset == null ? null : limitOffset.offset();
    }
}
