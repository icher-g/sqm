package io.cherlabs.sqm.core;

import io.cherlabs.sqm.core.traits.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represent a simple query.
 */
public class SelectQuery implements Query, HasLimit, HasOffset, HasDistinct, HasColumns, HasTable, HasJoins, HasGroupBy, HasOrderBy, HasWhere, HasHaving {

    private final List<Column> columns;
    private final List<Join> joins;
    private GroupBy groupBy;
    private OrderBy orderBy;
    private Table table;
    private Filter where;
    private Filter having;
    private Boolean distinct;
    private LimitOffset limitOffset;

    public SelectQuery() {
        this.columns = new ArrayList<>();
        this.joins = new ArrayList<>();
    }

    /**
     * Gets a list of the columns to be used in SELECT statement.
     *
     * @return a list of columns.
     */
    public List<Column> columns() {
        return columns;
    }

    /**
     * Adds columns to the SELECT statement.
     *
     * @param columns an array of columns.
     * @return this.
     */
    public SelectQuery select(Column... columns) {
        this.columns.addAll(List.of(columns));
        return this;
    }

    /**
     * Adds columns to the SELECT statement.
     *
     * @param columns a list of columns.
     * @return this.
     */
    public SelectQuery select(List<Column> columns) {
        this.columns.addAll(columns);
        return this;
    }

    /**
     * Gets a filter to be used in a WHERE statement.
     *
     * @return a filter.
     */
    public Filter where() {
        return where;
    }

    /**
     * Sets a filter to be used in a WHERE statement.
     *
     * @param filter a filter.
     * @return this.
     */
    public SelectQuery where(Filter filter) {
        this.where = filter;
        return this;
    }

    /**
     * Gets a filter to be used in a HAVING statement.
     *
     * @return a filter.
     */
    public Filter having() {
        return having;
    }

    /**
     * Sets a filter to be used in a HAVING statement.
     *
     * @param filter a filter.
     * @return this.
     */
    public SelectQuery having(Filter filter) {
        this.having = filter;
        return this;
    }

    /**
     * Gets a list of joins.
     *
     * @return a list of joins.
     */
    public List<Join> joins() {
        return joins;
    }

    /**
     * Adds joins to the query.
     *
     * @param joins an array of joins.
     * @return this.
     */
    public SelectQuery join(Join... joins) {
        this.joins.addAll(List.of(joins));
        return this;
    }

    /**
     * Adds joins to the query.
     *
     * @param joins a list of joins.
     * @return this.
     */
    public SelectQuery join(List<Join> joins) {
        this.joins.addAll(joins);
        return this;
    }

    /**
     * Gets a list of GroupBy items.
     *
     * @return a list of group by items.
     */
    public GroupBy groupBy() {
        return groupBy;
    }

    /**
     * Adds group by items to the query.
     *
     * @param items an array of group by items.
     * @return this.
     */
    public SelectQuery groupBy(Group... items) {
        return groupBy(List.of(items));
    }

    /**
     * Adds group by items to the query.
     *
     * @param items a list of group by items.
     * @return this.
     */
    public SelectQuery groupBy(List<Group> items) {
        if (items != null && !items.isEmpty()) {
            this.groupBy = new GroupBy(items);
        }
        return this;
    }

    /**
     * Gets a list of order by items.
     *
     * @return a list of oder by items.
     */
    public OrderBy orderBy() {
        return orderBy;
    }

    /**
     * Adds order by items to the query.
     *
     * @param items an array of order by items.
     * @return this.
     */
    public SelectQuery orderBy(Order... items) {
        return orderBy(List.of(items));
    }

    /**
     * Adds order by items to the query.
     *
     * @param items a list of group by items.
     * @return this.
     */
    public SelectQuery orderBy(List<Order> items) {
        if (items != null && !items.isEmpty()) {
            this.orderBy = new OrderBy(items);
        }
        return this;
    }

    /**
     * Gets a table used in a FROM statement.
     *
     * @return a table.
     */
    public Table table() {
        return table;
    }

    /**
     * Sets a table used in a FROM statement. The table can be a sub query.
     *
     * @param table a table.
     * @return this.
     */
    public SelectQuery from(Table table) {
        this.table = Objects.requireNonNull(table, "table");
        return this;
    }

    /**
     * Indicates if a DISTINCT keyword should be added to a SELECT statement.
     *
     * @return TRUE if a distinct needs to be added or FALSE otherwise.
     */
    public Boolean distinct() {
        return distinct;
    }

    /**
     * Sets distinct used in SELECT statement.
     *
     * @param distinct a value.
     * @return this.
     */
    public SelectQuery distinct(boolean distinct) {
        this.distinct = distinct;
        return this;
    }

    /**
     * Gets a limit of the query if there is any or NULL otherwise.
     *
     * @return a value of the limit.
     */
    public Long limit() {
        return limitOffset == null ? null : limitOffset.limit();
    }

    /**
     * Sets the query limit.
     *
     * @param limit a limit.
     * @return this.
     */
    public SelectQuery limit(long limit) {
        this.limitOffset = LimitOffset.of(limit, offset());
        return this;
    }

    /**
     * Gets an offset of the query if there is any or NULL otherwise.
     *
     * @return a value of the offset.
     */
    public Long offset() {
        return limitOffset == null ? null : limitOffset.offset();
    }

    /**
     * Sets the query offset.
     *
     * @param offset an offset.
     * @return this.
     */
    public SelectQuery offset(long offset) {
        this.limitOffset = LimitOffset.of(limit(), offset);
        return this;
    }
}
