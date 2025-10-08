package io.cherlabs.sqm.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a base class for all query implementations.
 *
 * @param <Q> An actual type of the derived class. This is used to support fluent API.
 */
public abstract class Query<Q extends Query<Q>> implements Entity {
    private final List<Column> columns;
    private final List<Join> joins;
    private final List<Group> groupBy;
    private final List<Order> orderBy;
    private String name;
    private Table table;
    private Filter where;
    private Filter having;
    private Boolean distinct;
    private Long limit;
    private Long offset;

    public Query() {
        this.columns = new ArrayList<>();
        this.joins = new ArrayList<>();
        this.groupBy = new ArrayList<>();
        this.orderBy = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    protected final Q self() {
        return (Q) this;
    }

    /**
     * The name of the query. This field is required in WITH clause for each CTE query.
     *
     * @return the name of the query if exists or NULL.
     */
    public String name() {
        return this.name;
    }

    /**
     * Sets a name for the query.
     *
     * @param name a name.
     * @return this.
     */
    public Q name(String name) {
        this.name = name;
        return self();
    }

    /**
     * Gets a list of the columns to be used in SELECT statement.
     *
     * @return a list of columns.
     */
    public List<Column> select() {
        return columns;
    }

    /**
     * Adds columns to the SELECT statement.
     *
     * @param columns an array of columns.
     * @return this.
     */
    public Q select(Column... columns) {
        this.columns.addAll(List.of(columns));
        return self();
    }

    /**
     * Adds columns to the SELECT statement.
     *
     * @param columns a list of columns.
     * @return this.
     */
    public Q select(List<Column> columns) {
        this.columns.addAll(columns);
        return self();
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
    public Q where(Filter filter) {
        this.where = filter;
        return self();
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
    public Q having(Filter filter) {
        this.having = filter;
        return self();
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
    public Q join(Join... joins) {
        this.joins.addAll(List.of(joins));
        return self();
    }

    /**
     * Adds joins to the query.
     *
     * @param joins a list of joins.
     * @return this.
     */
    public Q join(List<Join> joins) {
        this.joins.addAll(joins);
        return self();
    }

    /**
     * Gets a list of GroupBy items.
     *
     * @return a list of group by items.
     */
    public List<Group> groupBy() {
        return groupBy;
    }

    /**
     * Adds group by items to the query.
     *
     * @param items an array of group by items.
     * @return this.
     */
    public Q groupBy(Group... items) {
        this.groupBy.addAll(List.of(items));
        return self();
    }

    /**
     * Adds group by items to the query.
     *
     * @param items a list of group by items.
     * @return this.
     */
    public Q groupBy(List<Group> items) {
        this.groupBy.addAll(items);
        return self();
    }

    /**
     * Gets a list of order by items.
     *
     * @return a list of oder by items.
     */
    public List<Order> orderBy() {
        return orderBy;
    }

    /**
     * Adds order by items to the query.
     *
     * @param items an array of order by items.
     * @return this.
     */
    public Q orderBy(Order... items) {
        this.orderBy.addAll(List.of(items));
        return self();
    }

    /**
     * Adds order by items to the query.
     *
     * @param items a list of group by items.
     * @return this.
     */
    public Q orderBy(List<Order> items) {
        this.orderBy.addAll(items);
        return self();
    }

    /**
     * Gets a table used in a FROM statement.
     *
     * @return a table.
     */
    public Table from() {
        return table;
    }

    /**
     * Sets a table used in a FROM statement. The table can be a sub query.
     *
     * @param table a table.
     * @return this.
     */
    public Q from(Table table) {
        this.table = Objects.requireNonNull(table, "table");
        return self();
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
    public Q distinct(boolean distinct) {
        this.distinct = distinct;
        return self();
    }

    /**
     * Gets a limit of the query if there is any or NULL otherwise.
     *
     * @return a value of the limit.
     */
    public Long limit() {
        return limit;
    }

    /**
     * Sets the query limit.
     *
     * @param limit a limit.
     * @return this.
     */
    public Q limit(long limit) {
        this.limit = limit;
        return self();
    }

    /**
     * Gets an offset of the query if there is any or NULL otherwise.
     *
     * @return a value of the offset.
     */
    public Long offset() {
        return offset;
    }

    /**
     * Sets the query offset.
     *
     * @param offset an offset.
     * @return this.
     */
    public Q offset(long offset) {
        this.offset = offset;
        return self();
    }
}
