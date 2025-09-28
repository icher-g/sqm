package io.cherlabs.sqlmodel.dsl;

import io.cherlabs.sqlmodel.core.*;
import io.cherlabs.sqlmodel.core.views.Columns;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Minimal, ergonomic, static-import friendly helpers to build the core model
 * without touching parsers. Keep names short and predictable.
 * <p>
 * Usage (static import io.cherlabs.sqlmodel.dsl.DSL.*):
 * <p>
 * {@code
 * Query q = q()
 * .select(c("t","id"), c("t","name").as("n"),
 * func("lower", argCol(c("t","name"))).as("nm"))
 * .from(t("users").as("t"))
 * .join(inner(t("orders").as("o")).on(join(c("t","id")).eq(c("o","user_id"))))
 * .where(and(eq(c("t","active"), true), inVals(c("t","role"), List.of("admin","user"))))
 * .groupBy(g(c("t","role")))
 * .orderBy(asc(c("t","name")))
 * .limit(100).offset(0);
 * }
 */
public final class DSL {
    private DSL() {
    }

    /* ========================= Tables ========================= */

    public static NamedTable t(String name) {
        return Table.of(name);
    }

    public static NamedTable t(String schema, String name) {
        return Table.of(name).from(schema);
    }

    public static NamedTable t(String schema, String name, String alias) {
        return Table.of(name).from(schema).as(alias);
    }

    public static NamedTable tAs(String name, String alias) {
        return Table.of(name).as(alias);
    }

    /* ========================= Columns ========================= */

    public static NamedColumn c(String name) {
        return Column.of(name);
    }

    public static NamedColumn c(String table, String name) {
        return Column.of(name).from(table);
    }

    public static NamedColumn cAs(String name, String alias) {
        return Column.of(name).as(alias);
    }

    public static NamedColumn cAs(String table, String name, String alias) {
        return Column.of(name).from(table).as(alias);
    }

    public static QueryColumn c(Query<?> subquery) {
        return Column.of(subquery);
    }

    public static ExpressionColumn expr(String sqlExpression) {
        return Column.expr(sqlExpression);
    }

    /* ========================= Functions ========================= */

    public static FunctionColumn func(String name, FunctionColumn.Arg... args) {
        return Column.func(name, args);
    }

    public static FunctionColumn.Arg argCol(Column col) {
        return FunctionColumn.Arg.column(Columns.table(col).orElse(null), Columns.name(col).orElse(null));
    }

    public static FunctionColumn.Arg argLit(Object value) {
        return FunctionColumn.Arg.lit(value);
    }

    public static FunctionColumn.Arg argFunc(FunctionColumn call) {
        return FunctionColumn.Arg.func(call);
    }

    public static FunctionColumn.Arg star() {
        return FunctionColumn.Arg.star();
    }

    /* ========================= CASE ========================= */

    public static CaseColumn kase(WhenThen... whens) { // "case" is reserved in Java
        return CaseColumn.of(List.of(whens));
    }

    public static WhenThen when(Filter condition, Entity thenValue) {
        return CaseColumn.when(condition, thenValue);
    }

    public static WhenThen when(Filter condition) {
        return WhenThen.when(condition);
    }

    public static WhenThen then(WhenThen w, Entity value) {
        return w.then(value);
    }

    public static CaseColumn kaseElse(CaseColumn c, Entity elseValue) {
        return c.elseValue(elseValue);
    }

    /* ========================= Filters (Column RHS values) ========================= */

    public static Values.Single val(Object value) {
        return Values.single(value);
    }

    public static Values.ListValues vals(List<Object> values) {
        return Values.list(values);
    }

    public static Values.ListValues vals(Object... values) {
        return Values.list(Arrays.asList(values));
    }

    public static Values.Range range(Object min, Object max) {
        return Values.range(min, max);
    }

    public static Values.Subquery subq(Query<?> q) {
        return Values.subquery(q);
    }

    public static Values.Tuples tuples(List<List<Object>> rows) {
        return Values.tuples(rows);
    }

    /* ========================= Filters (predicates) ========================= */

    public static ColumnFilter colFilter(Column col) {
        return Filter.column(col);
    }

    public static TupleFilter tupleFilter(List<Column> cols) {
        return Filter.tuple(cols);
    }

    // column operators
    public static ColumnFilter eq(Column col, Object value) {
        return Filter.column(col).eq(value);
    }

    public static ColumnFilter eq(Column col1, Column col2) {
        return Filter.column(col1).eq(col2);
    }

    public static ColumnFilter ne(Column col, Object value) {
        return Filter.column(col).ne(value);
    }

    public static ColumnFilter ne(Column col1, Column col2) {
        return Filter.column(col1).ne(col2);
    }

    public static ColumnFilter lt(Column col, Object value) {
        return Filter.column(col).lt(value);
    }

    public static ColumnFilter lt(Column col1, Column col2) {
        return Filter.column(col1).lt(col2);
    }

    public static ColumnFilter lte(Column col, Object value) {
        return Filter.column(col).lte(value);
    }

    public static ColumnFilter lte(Column col1, Column col2) {
        return Filter.column(col1).lte(col2);
    }

    public static ColumnFilter gt(Column col, Object value) {
        return Filter.column(col).gt(value);
    }

    public static ColumnFilter gt(Column col1, Column col2) {
        return Filter.column(col1).gt(col2);
    }

    public static ColumnFilter gte(Column col, Object value) {
        return Filter.column(col).gte(value);
    }

    public static ColumnFilter gte(Column col1, Column col2) {
        return Filter.column(col1).gte(col2);
    }

    public static ColumnFilter in(Column col, List<?> values) {
        return Filter.column(col).in(values.stream().map(Object.class::cast).collect(Collectors.toList()));
    }

    public static ColumnFilter notIn(Column col, List<?> values) {
        return Filter.column(col).notIn(values.stream().map(Object.class::cast).collect(Collectors.toList()));
    }

    public static ColumnFilter between(Column col, Object min, Object max) {
        return Filter.column(col).range(min, max);
    }

    // tuple IN / NOT IN
    public static TupleFilter in(List<Column> cols, List<List<Object>> rows) {
        return Filter.tuple(cols).in(rows);
    }

    public static TupleFilter notIn(List<Column> cols, List<List<Object>> rows) {
        return Filter.tuple(cols).notIn(rows);
    }

    // composite
    public static CompositeFilter and(Filter... filters) {
        return Filter.and(filters);
    }

    public static CompositeFilter and(List<Filter> filters) {
        return Filter.and(filters);
    }

    public static CompositeFilter or(Filter... filters) {
        return Filter.or(filters);
    }

    public static CompositeFilter or(List<Filter> filters) {
        return Filter.or(filters);
    }

    public static CompositeFilter not(Filter filter) {
        return Filter.not(filter);
    }

    // join predicate helpers (column vs column)

    /* ========================= Joins ========================= */

    public static TableJoin inner(Table table) {
        return Join.inner(table);
    }

    public static TableJoin left(Table table) {
        return Join.left(table);
    }

    public static TableJoin right(Table table) {
        return Join.right(table);
    }

    public static TableJoin full(Table table) {
        return Join.full(table);
    }

    public static TableJoin cross(Table table) {
        return Join.cross(table);
    }

    public static TableJoin on(TableJoin j, Filter on) {
        return j.on(on);
    }

    public static ExpressionJoin joinExpr(String expression) {
        return Join.expr(expression);
    }

    /* ========================= GROUP BY / ORDER BY ========================= */

    public static Group g(Column col) {
        return Group.by(col);
    }

    public static Group g(int ordinal) {
        return Group.ofOrdinal(ordinal);
    }

    public static Order o(Column col) {
        return Order.by(col);
    }

    public static Order asc(Column col) {
        return Order.by(col).asc();
    }

    public static Order desc(Column col) {
        return Order.by(col).desc();
    }

    public static Order nulls(Order oi, Nulls n) {
        return new Order(oi.column(), oi.direction(), n, oi.collate());
    }

    public static Order collate(Order oi, String locale) {
        return new Order(oi.column(), oi.direction(), oi.nulls(), locale);
    }

    /* ========================= Query ========================= */

    public static SelectQuery q() {
        return new SelectQuery();
    }

    public static Query<?> select(Query<?> q, Column... cols) {
        return q.select(List.of(cols));
    }

    public static Query<?> select(Query<?> q, List<Column> cols) {
        return q.select(cols);
    }

    public static Query<?> from(Query<?> q, Table table) {
        return q.from(table);
    }

    public static Query<?> where(Query<?> q, Filter filter) {
        return q.where(filter);
    }

    public static Query<?> having(Query<?> q, Filter filter) {
        return q.having(filter);
    }

    public static Query<?> join(Query<?> q, Join... joins) {
        return q.join(List.of(joins));
    }

    public static Query<?> join(Query<?> q, List<Join> joins) {
        return q.join(joins);
    }

    public static Query<?> groupBy(Query<?> q, Group... items) {
        return q.groupBy(List.of(items));
    }

    public static Query<?> groupBy(Query<?> q, List<Group> items) {
        return q.groupBy(items);
    }

    public static Query<?> orderBy(Query<?> q, Order... items) {
        return q.orderBy(List.of(items));
    }

    public static Query<?> orderBy(Query<?> q, List<Order> items) {
        return q.orderBy(items);
    }

    public static Query<?> distinct(Query<?> q) {
        return q.distinct(true);
    }

    public static Query<?> limit(Query<?> q, long limit) {
        return q.limit(limit);
    }

    public static Query<?> offset(Query<?> q, long offset) {
        return q.offset(offset);
    }
}
