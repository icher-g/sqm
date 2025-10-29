package io.sqm.core.internal;

import io.sqm.core.*;

import java.util.ArrayList;
import java.util.List;

public class SelectQueryImpl implements SelectQuery {

    private final List<SelectItem> items;
    private final List<Join> joins;
    private GroupBy groupBy;
    private OrderBy orderBy;
    private TableRef tableRef;
    private Predicate where;
    private Predicate having;
    private Boolean distinct;
    private LimitOffset limitOffset;

    public SelectQueryImpl() {
        this.items = new ArrayList<>();
        this.joins = new ArrayList<>();
    }

    /**
     * Gets a list of select items to be used in SELECT statement.
     *
     * @return a list of select items.
     */
    @Override
    public List<SelectItem> select() {
        return items;
    }

    /**
     * Adds items to the SELECT statement.
     *
     * @param items a list of items.
     * @return this.
     */
    @Override
    public SelectQuery select(List<SelectItem> items) {
        this.items.addAll(items);
        return this;
    }

    /**
     * Gets a table reference used in a FROM statement.
     *
     * @return a table.
     */
    @Override
    public TableRef from() {
        return tableRef;
    }

    /**
     * Sets a table reference used in a FROM statement. The table can be a sub query.
     *
     * @param table a table reference.
     * @return this.
     */
    @Override
    public SelectQuery from(TableRef table) {
        this.tableRef = table;
        return this;
    }

    /**
     * Gets a list of joins.
     *
     * @return a list of joins.
     */
    @Override
    public List<Join> joins() {
        return joins;
    }

    /**
     * Adds joins to the query.
     *
     * @param joins a list of joins.
     * @return this.
     */
    @Override
    public SelectQuery join(List<Join> joins) {
        this.joins.addAll(joins);
        return this;
    }

    /**
     * Gets a predicate to be used in a WHERE statement.
     *
     * @return a predicate.
     */
    @Override
    public Predicate where() {
        return where;
    }

    /**
     * Sets a predicate to be used in a WHERE statement.
     *
     * @param predicate a predicate.
     * @return this.
     */
    @Override
    public SelectQuery where(Predicate predicate) {
        this.where = predicate;
        return this;
    }

    /**
     * Gets a list of GroupBy items.
     *
     * @return a list of group by items.
     */
    @Override
    public GroupBy groupBy() {
        return groupBy;
    }

    /**
     * Adds group by items to the query.
     *
     * @param items a list of group by items.
     * @return this.
     */
    @Override
    public SelectQuery groupBy(List<GroupItem> items) {
        this.groupBy = new GroupByImpl(items);
        return this;
    }

    /**
     * Gets a predicate to be used in a HAVING statement.
     *
     * @return a predicate.
     */
    @Override
    public Predicate having() {
        return having;
    }

    /**
     * Sets a predicate to be used in a HAVING statement.
     *
     * @param predicate a predicate.
     * @return this.
     */
    @Override
    public SelectQuery having(Predicate predicate) {
        this.having = predicate;
        return this;
    }

    /**
     * Gets a list of order by items.
     *
     * @return a list of oder by items.
     */
    @Override
    public OrderBy orderBy() {
        return orderBy;
    }

    /**
     * Adds order by items to the query.
     *
     * @param items a list of group by items.
     * @return this.
     */
    @Override
    public SelectQuery orderBy(List<OrderItem> items) {
        this.orderBy = new OrderByImpl(items);
        return this;
    }

    /**
     * Indicates if a DISTINCT keyword should be added to a SELECT statement.
     *
     * @return TRUE if a distinct needs to be added or FALSE otherwise.
     */
    @Override
    public Boolean distinct() {
        return distinct;
    }

    /**
     * Sets distinct used in SELECT statement.
     *
     * @param distinct a value.
     * @return this.
     */
    @Override
    public SelectQuery distinct(boolean distinct) {
        this.distinct = distinct;
        return this;
    }

    /**
     * Gets a limit of the query if there is any or NULL otherwise.
     *
     * @return a value of the limit.
     */
    @Override
    public Long limit() {
        return limitOffset == null ? null : limitOffset.limit();
    }

    /**
     * Sets the query limit.
     *
     * @param limit a limit.
     * @return this.
     */
    @Override
    public SelectQuery limit(long limit) {
        this.limitOffset = LimitOffset.of(limit, offset());
        return this;
    }

    /**
     * Gets an offset of the query if there is any or NULL otherwise.
     *
     * @return a value of the offset.
     */
    @Override
    public Long offset() {
        return limitOffset == null ? null : limitOffset.offset();
    }

    /**
     * Sets the query offset.
     *
     * @param offset an offset.
     * @return this.
     */
    @Override
    public SelectQuery offset(long offset) {
        this.limitOffset = LimitOffset.of(limit(), offset);
        return this;
    }
}
