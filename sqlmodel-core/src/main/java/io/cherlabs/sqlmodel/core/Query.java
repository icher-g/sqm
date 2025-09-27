package io.cherlabs.sqlmodel.core;

import java.util.*;

public class Query implements Entity {

    private final List<Column> columns;
    private final List<Join> joins;
    private final List<GroupItem> groupBy;
    private final List<OrderItem> orderBy;
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

    public List<Column> select() {
        return columns;
    }

    public Query select(Column... columns) {
        this.columns.addAll(List.of(columns));
        return this;
    }

    public Query select(List<Column> columns) {
        this.columns.addAll(columns);
        return this;
    }

    public Filter where() {
        return where;
    }

    public Query where(Filter filter) {
        this.where = filter;
        return this;
    }

    public Filter having() {
        return having;
    }

    public Query having(Filter filter) {
        this.having = filter;
        return this;
    }

    public List<Join> joins() {
        return joins;
    }

    public Query join(Join... joins) {
        this.joins.addAll(List.of(joins));
        return this;
    }

    public Query join(List<Join> joins) {
        this.joins.addAll(joins);
        return this;
    }

    public List<GroupItem> groupBy() {
        return groupBy;
    }

    public Query groupBy(GroupItem... items) {
        this.groupBy.addAll(List.of(items));
        return this;
    }

    public Query groupBy(List<GroupItem> items) {
        this.groupBy.addAll(items);
        return this;
    }

    public List<OrderItem> orderBy() {
        return orderBy;
    }

    public Query orderBy(OrderItem... items) {
        this.orderBy.addAll(List.of(items));
        return this;
    }

    public Query orderBy(List<OrderItem> items) {
        this.orderBy.addAll(items);
        return this;
    }

    public Table from() {
        return table;
    }

    public Query from(Table table) {
        this.table = Objects.requireNonNull(table, "table");
        return this;
    }

    public Boolean distinct() {
        return distinct;
    }

    public Query distinct(boolean distinct) {
        this.distinct = distinct;
        return this;
    }

    public Long limit() {
        return limit;
    }

    public Query limit(long limit) {
        this.limit = limit;
        return this;
    }

    public Long offset() {
        return offset;
    }

    public Query offset(long offset) {
        this.offset = offset;
        return this;
    }
}
