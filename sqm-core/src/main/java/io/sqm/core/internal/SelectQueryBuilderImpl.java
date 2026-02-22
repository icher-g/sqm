package io.sqm.core.internal;

import io.sqm.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Internal mutable builder implementation for {@link SelectQuery}.
 */
public final class SelectQueryBuilderImpl implements SelectQueryBuilder {
    private final List<SelectItem> items = new ArrayList<>();
    private final List<Join> joins = new ArrayList<>();
    private final List<WindowDef> windows = new ArrayList<>();
    private GroupBy groupBy;
    private OrderBy orderBy;
    private TableRef tableRef;
    private Predicate where;
    private Predicate having;
    private DistinctSpec distinctSpec;
    private LimitOffset limitOffset;
    private LockingClause lockingClause;

    /**
     * Creates an empty builder.
     */
    public SelectQueryBuilderImpl() {
    }

    /**
     * Creates a builder initialized from an existing select query.
     *
     * @param query source query
     */
    public SelectQueryBuilderImpl(SelectQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        this.items.addAll(query.items());
        this.tableRef = query.from();
        this.joins.addAll(query.joins());
        this.where = query.where();
        this.groupBy = query.groupBy();
        this.having = query.having();
        this.orderBy = query.orderBy();
        this.distinctSpec = query.distinct();
        this.limitOffset = query.limitOffset();
        this.lockingClause = query.lockFor();
        this.windows.addAll(query.windows());
    }

    @Override
    public SelectQueryBuilder select(List<SelectItem> items) {
        Objects.requireNonNull(items, "items must not be null");
        this.items.addAll(items);
        return this;
    }

    @Override
    public SelectQueryBuilder from(TableRef tableRef) {
        this.tableRef = tableRef;
        return this;
    }

    @Override
    public SelectQueryBuilder join(List<Join> joins) {
        Objects.requireNonNull(joins, "joins must not be null");
        this.joins.addAll(joins);
        return this;
    }

    @Override
    public SelectQueryBuilder join(Join join) {
        Objects.requireNonNull(join, "join must not be null");
        this.joins.add(join);
        return this;
    }

    @Override
    public SelectQueryBuilder where(Predicate predicate) {
        this.where = predicate;
        return this;
    }

    @Override
    public SelectQueryBuilder groupBy(List<GroupItem> items) {
        Objects.requireNonNull(items, "items must not be null");
        this.groupBy = GroupBy.of(items);
        return this;
    }

    @Override
    public SelectQueryBuilder having(Predicate predicate) {
        this.having = predicate;
        return this;
    }

    @Override
    public SelectQueryBuilder window(List<WindowDef> windows) {
        Objects.requireNonNull(windows, "windows must not be null");
        this.windows.addAll(windows);
        return this;
    }

    @Override
    public SelectQueryBuilder window(WindowDef window) {
        Objects.requireNonNull(window, "window must not be null");
        this.windows.add(window);
        return this;
    }

    @Override
    public SelectQueryBuilder orderBy(List<OrderItem> items) {
        Objects.requireNonNull(items, "items must not be null");
        this.orderBy = OrderBy.of(items);
        return this;
    }

    @Override
    public SelectQueryBuilder distinct(DistinctSpec distinctSpec) {
        this.distinctSpec = distinctSpec;
        return this;
    }

    @Override
    public SelectQueryBuilder limitOffset(LimitOffset limitOffset) {
        this.limitOffset = limitOffset;
        return this;
    }

    @Override
    public LimitOffset currentLimitOffset() {
        return limitOffset;
    }

    @Override
    public SelectQueryBuilder lockFor(LockingClause lockingClause) {
        this.lockingClause = lockingClause;
        return this;
    }

    @Override
    public SelectQuery build() {
        return SelectQuery.of(
            items,
            tableRef,
            joins,
            where,
            groupBy,
            having,
            orderBy,
            distinctSpec,
            limitOffset,
            lockingClause,
            windows
        );
    }
}
