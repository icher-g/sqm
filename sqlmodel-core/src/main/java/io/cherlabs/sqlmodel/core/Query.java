package io.cherlabs.sqlmodel.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public String name() {
        return this.name;
    }

    public Q name(String name) {
        this.name = name;
        return self();
    }

    public List<Column> select() {
        return columns;
    }

    public Q select(Column... columns) {
        this.columns.addAll(List.of(columns));
        return self();
    }

    public Q select(List<Column> columns) {
        this.columns.addAll(columns);
        return self();
    }

    public Filter where() {
        return where;
    }

    public Q where(Filter filter) {
        this.where = filter;
        return self();
    }

    public Filter having() {
        return having;
    }

    public Q having(Filter filter) {
        this.having = filter;
        return self();
    }

    public List<Join> joins() {
        return joins;
    }

    public Q join(Join... joins) {
        this.joins.addAll(List.of(joins));
        return self();
    }

    public Q join(List<Join> joins) {
        this.joins.addAll(joins);
        return self();
    }

    public List<Group> groupBy() {
        return groupBy;
    }

    public Q groupBy(Group... items) {
        this.groupBy.addAll(List.of(items));
        return self();
    }

    public Q groupBy(List<Group> items) {
        this.groupBy.addAll(items);
        return self();
    }

    public List<Order> orderBy() {
        return orderBy;
    }

    public Q orderBy(Order... items) {
        this.orderBy.addAll(List.of(items));
        return self();
    }

    public Q orderBy(List<Order> items) {
        this.orderBy.addAll(items);
        return self();
    }

    public Table from() {
        return table;
    }

    public Q from(Table table) {
        this.table = Objects.requireNonNull(table, "table");
        return self();
    }

    public Boolean distinct() {
        return distinct;
    }

    public Q distinct(boolean distinct) {
        this.distinct = distinct;
        return self();
    }

    public Long limit() {
        return limit;
    }

    public Q limit(long limit) {
        this.limit = limit;
        return self();
    }

    public Long offset() {
        return offset;
    }

    public Q offset(long offset) {
        this.offset = offset;
        return self();
    }
}
