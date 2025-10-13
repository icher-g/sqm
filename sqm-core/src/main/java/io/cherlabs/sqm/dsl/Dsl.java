package io.cherlabs.sqm.dsl;

import io.cherlabs.sqm.core.*;
import io.cherlabs.sqm.core.views.Columns;

import java.util.Arrays;
import java.util.List;

/**
 * Minimal, ergonomic, static-import friendly helpers to build the core model
 * without touching parsers. Keep names short and predictable.
 * <p>
 * Usage (static import io.cherlabs.sqm.dsl.DSL.*):
 * <p>
 * <pre>
 * {@code
 *   Query q = q()
 *   .select(c("t","id"), c("t","name").as("n"), func("lower", argCol(c("t","name"))).as("nm"))
 *   .from(t("users").as("t"))
 *   .join(inner(t("orders").as("o")).on(join(c("t","id")).eq(c("o","user_id"))))
 *   .where(and(eq(c("t","active"), true), in(c("t","role"), List.of("admin","user"))))
 *   .groupBy(g(c("t","role")))
 *   .orderBy(asc(c("t","name")))
 *   .limit(100)
 *   .offset(0);
 * }
 * </pre>
 */
public final class Dsl {
    private Dsl() {
    }

    /* ========================= Tables ========================= */

    /**
     * Creates a table with the provided name.
     *
     * @param name a name of the table.
     * @return a table.
     */
    public static NamedTable tbl(String name) {
        return Table.of(name);
    }

    /**
     * Creates a table with the provided schema and name.
     *
     * @param schema a table schema.
     * @param name   a table name.
     * @return a table.
     */
    public static NamedTable tbl(String schema, String name) {
        return Table.of(name).from(schema);
    }

    /* ========================= Columns ========================= */

    /**
     * Creates a column with the provided name.
     *
     * @param name a column name.
     * @return a column.
     */
    public static NamedColumn col(String name) {
        return Column.of(name);
    }

    /**
     * Creates a column with the provided table and name.
     *
     * @param table a column table.
     * @param name  a column name.
     * @return a column.
     */
    public static NamedColumn col(String table, String name) {
        return Column.of(name).from(table);
    }

    /**
     * Creates a column that is represented by a sub query.
     *
     * @param subquery a sub query.
     * @return a query column.
     */
    public static QueryColumn col(Query subquery) {
        return Column.of(subquery);
    }

    /**
     * Creates a column that is represented by a string expression.
     *
     * @param sqlExpression an expression.
     * @return an expression column.
     */
    public static ExpressionColumn expr(String sqlExpression) {
        return Column.expr(sqlExpression);
    }

    /* ========================= Functions ========================= */

    /**
     * Creates a column that represents a function call.
     *
     * @param name a name of the function.
     * @param args an array of arguments for the function. An array can be empty if a function does not accept any arguments.
     * @return a function column.
     */
    public static FunctionColumn func(String name, FunctionColumn.Arg... args) {
        return Column.func(name, args);
    }

    /**
     * Creates a function argument represented by a column.
     *
     * @param col a column to be passed to a function.
     * @return a function argument.
     */
    public static FunctionColumn.Arg argCol(Column col) {
        return FunctionColumn.Arg.column(Columns.table(col).orElse(null), Columns.name(col).orElse(null));
    }

    /**
     * Creates a function argument represented by a literal.
     *
     * @param value a literal value.
     * @return a function argument.
     */
    public static FunctionColumn.Arg argLit(Object value) {
        return FunctionColumn.Arg.lit(value);
    }

    /**
     * Creates a function argument represented by a nested function call.
     *
     * @param call a nested function call.
     * @return a function argument.
     */
    public static FunctionColumn.Arg argFunc(FunctionColumn call) {
        return FunctionColumn.Arg.func(call);
    }

    /**
     * Creates a function argument represented by a start '*'.
     *
     * @return a function argument.
     */
    public static FunctionColumn.Arg star() {
        return FunctionColumn.Arg.star();
    }

    /* ========================= CASE ========================= */

    /**
     * Creates a column that represents a CASE statement.
     *
     * @param whens an array of WHEN...THEN groups.
     * @return a case column.
     */
    public static CaseColumn kase(WhenThen... whens) { // "case" is reserved in Java
        return CaseColumn.of(List.of(whens));
    }

    /**
     * Creates a WHEN...THEN statement used in the CASE.
     *
     * @param condition a condition used in the WHEN statement.
     * @param thenValue a value returned by the statement if the WHEN statement returns true.
     * @return a WhenThen object.
     */
    public static WhenThen when(Filter condition, Entity thenValue) {
        return CaseColumn.when(condition, thenValue);
    }

    /**
     * Creates a WHEN...THEN statement with only WHEN.
     *
     * @param condition a condition used in the WHEN statement.
     * @return a WhenThen object.
     */
    public static WhenThen when(Filter condition) {
        return WhenThen.when(condition);
    }

    /**
     * Adds a THEN statement to a previously created WHEN.
     *
     * @param w     a WhenThen object to add the THEN to.
     * @param value a THEN value.
     * @return a WhenThen object.
     */
    public static WhenThen then(WhenThen w, Entity value) {
        return w.then(value);
    }

    /**
     * Adds an ELSE value to a column that represents a CASE statement.
     *
     * @param c         a case column.
     * @param elseValue an else value.
     * @return a case column.
     */
    public static CaseColumn kaseElse(CaseColumn c, Entity elseValue) {
        return c.elseValue(elseValue);
    }

    /* ========================= Filters (Column RHS values) ========================= */

    /**
     * Creates a single value.
     *
     * @param value a value.
     * @return Values that represents a single value.
     */
    public static Values.Single val(Object value) {
        return Values.single(value);
    }

    /**
     * Creates a list of values. Can be used in an IN filter.
     *
     * @param values a list of values.
     * @return Values that represents a list of values.
     */
    public static Values.ListValues vals(List<Object> values) {
        return Values.list(values);
    }

    /**
     * Creates a list of values. Can be used in an IN filter.
     *
     * @param values an array of values.
     * @return Values that represents a list of values.
     */
    public static Values.ListValues vals(Object... values) {
        return Values.list(Arrays.asList(values));
    }

    /**
     * Creates a values range. Used in BETWEEN statement.
     *
     * @param min a minimum value.
     * @param max a maximum value.
     * @return Values that represents a range.
     */
    public static Values.Range range(Object min, Object max) {
        return Values.range(min, max);
    }

    /**
     * Creates a sub query value.
     * For example: {@code WHERE c1 IN (SELECT ID FROM t)}
     *
     * @param q a sbu query.
     * @return Values that represents a sub query.
     */
    public static Values.Subquery val(Query q) {
        return Values.subquery(q);
    }

    /**
     * Creates a list of tuples.
     * For example: {@code WHERE (c1, c2) IN ((1, 2), (3, 4))}
     *
     * @param rows a list of tuples.
     * @return Values that represents a list of tuples.
     */
    public static Values.Tuples tuples(List<List<Object>> rows) {
        return Values.tuples(rows);
    }

    /* ========================= column operators ========================= */

    /**
     * Creates column filter with {@link io.cherlabs.sqm.core.ColumnFilter.Operator#Eq} op and value.
     * For example: {@code WHERE c1 = 1}
     *
     * @param col   a column.
     * @param value a value.
     * @return a column filter.
     */
    public static ColumnFilter eq(Column col, Object value) {
        return Filter.column(col).eq(value);
    }

    /**
     * Creates column filter with {@link io.cherlabs.sqm.core.ColumnFilter.Operator#Eq} op between two columns.
     * For example: {@code WHERE c1 = c2}
     *
     * @param col1 a left column.
     * @param col2 a right column.
     * @return a column filter.
     */
    public static ColumnFilter eq(Column col1, Column col2) {
        return Filter.column(col1).eq(col2);
    }

    /**
     * Creates column filter with {@link io.cherlabs.sqm.core.ColumnFilter.Operator#Ne} op and value.
     * For example: {@code WHERE c1 <> 1}
     *
     * @param col   a column.
     * @param value a value.
     * @return a column filter.
     */
    public static ColumnFilter ne(Column col, Object value) {
        return Filter.column(col).ne(value);
    }

    /**
     * Creates column filter with {@link io.cherlabs.sqm.core.ColumnFilter.Operator#Ne} op between two columns.
     * For example: {@code WHERE c1 <> c2}
     *
     * @param col1 a left column.
     * @param col2 a right column.
     * @return a column filter.
     */
    public static ColumnFilter ne(Column col1, Column col2) {
        return Filter.column(col1).ne(col2);
    }

    /**
     * Creates column filter with {@link io.cherlabs.sqm.core.ColumnFilter.Operator#Lt} op and value.
     * For example: {@code WHERE c1 < 1}
     *
     * @param col   a column.
     * @param value a value.
     * @return a column filter.
     */
    public static ColumnFilter lt(Column col, Object value) {
        return Filter.column(col).lt(value);
    }

    /**
     * Creates column filter with {@link io.cherlabs.sqm.core.ColumnFilter.Operator#Lt} op between two columns.
     * For example: {@code WHERE c1 < c2}
     *
     * @param col1 a left column.
     * @param col2 a right column.
     * @return a column filter.
     */
    public static ColumnFilter lt(Column col1, Column col2) {
        return Filter.column(col1).lt(col2);
    }

    /**
     * Creates column filter with {@link io.cherlabs.sqm.core.ColumnFilter.Operator#Lte} op and value.
     * For example: {@code WHERE c1 <= 1}
     *
     * @param col   a column.
     * @param value a value.
     * @return a column filter.
     */
    public static ColumnFilter lte(Column col, Object value) {
        return Filter.column(col).lte(value);
    }

    /**
     * Creates column filter with {@link io.cherlabs.sqm.core.ColumnFilter.Operator#Lte} op between two columns.
     * For example: {@code WHERE c1 <= c2}
     *
     * @param col1 a left column.
     * @param col2 a right column.
     * @return a column filter.
     */
    public static ColumnFilter lte(Column col1, Column col2) {
        return Filter.column(col1).lte(col2);
    }

    /**
     * Creates column filter with {@link io.cherlabs.sqm.core.ColumnFilter.Operator#Gt} op and value.
     * For example: {@code WHERE c1 > 1}
     *
     * @param col   a column.
     * @param value a value.
     * @return a column filter.
     */
    public static ColumnFilter gt(Column col, Object value) {
        return Filter.column(col).gt(value);
    }

    /**
     * Creates column filter with {@link io.cherlabs.sqm.core.ColumnFilter.Operator#Gt} op between two columns.
     * For example: {@code WHERE c1 > c2}
     *
     * @param col1 a left column.
     * @param col2 a right column.
     * @return a column filter.
     */
    public static ColumnFilter gt(Column col1, Column col2) {
        return Filter.column(col1).gt(col2);
    }

    /**
     * Creates column filter with {@link io.cherlabs.sqm.core.ColumnFilter.Operator#Gte} op and value.
     * For example: {@code WHERE c1 >= 1}
     *
     * @param col   a column.
     * @param value a value.
     * @return a column filter.
     */
    public static ColumnFilter gte(Column col, Object value) {
        return Filter.column(col).gte(value);
    }

    /**
     * Creates column filter with {@link io.cherlabs.sqm.core.ColumnFilter.Operator#Gte} op between two columns.
     * For example: {@code WHERE c1 >= c2}
     *
     * @param col1 a left column.
     * @param col2 a right column.
     * @return a column filter.
     */
    public static ColumnFilter gte(Column col1, Column col2) {
        return Filter.column(col1).gte(col2);
    }

    /**
     * Creates column filter with {@link io.cherlabs.sqm.core.ColumnFilter.Operator#In} op and values.
     * For example: {@code WHERE c1 IN (1, 2, 3)}
     *
     * @param col    a column.
     * @param values a list of values.
     * @return a column filter.
     */
    public static ColumnFilter in(Column col, List<Object> values) {
        return Filter.column(col).in(values);
    }

    /**
     * Creates column filter with {@link io.cherlabs.sqm.core.ColumnFilter.Operator#In} op and values.
     * For example: {@code WHERE c1 IN (1, 2, 3)}
     *
     * @param col    a column.
     * @param values a list of values.
     * @return a column filter.
     */
    public static ColumnFilter in(Column col, Object... values) {
        return Filter.column(col).in(values);
    }

    /**
     * Creates column filter with {@link io.cherlabs.sqm.core.ColumnFilter.Operator#NotIn} op and values.
     * For example: {@code WHERE c1 NOT IN (1, 2, 3)}
     *
     * @param col    a column.
     * @param values a list of values.
     * @return a column filter.
     */
    public static ColumnFilter notIn(Column col, List<Object> values) {
        return Filter.column(col).notIn(values);
    }

    /**
     * Creates column filter with {@link io.cherlabs.sqm.core.ColumnFilter.Operator#NotIn} op and values.
     * For example: {@code WHERE c1 NOT IN (1, 2, 3)}
     *
     * @param col    a column.
     * @param values a list of values.
     * @return a column filter.
     */
    public static ColumnFilter notIn(Column col, Object... values) {
        return Filter.column(col).notIn(values);
    }

    /**
     * Creates column filter with {@link io.cherlabs.sqm.core.ColumnFilter.Operator#Range} op.
     * For example: {@code c1 BETWEEN 1 AND 100}
     *
     * @param col a column.
     * @param min a minimum value.
     * @param max a maximum value.
     * @return a column filter.
     */
    public static ColumnFilter between(Column col, Object min, Object max) {
        return Filter.column(col).range(min, max);
    }

    /**
     * Creates a tuple filter with {@link io.cherlabs.sqm.core.TupleFilter.Operator#In} op.
     * For example: {@code (c1, c2) IN ((1, 2), (3, 4))}
     *
     * @param cols a list of columns on the left side of the filter.
     * @param rows a list of tuple values.
     * @return a tuple filter.
     */
    public static TupleFilter in(List<Column> cols, List<List<Object>> rows) {
        return Filter.tuple(cols).in(rows);
    }

    /**
     * Creates a tuple filter with {@link io.cherlabs.sqm.core.TupleFilter.Operator#NotIn} op.
     * For example: {@code (c1, c2) NOT IN ((1, 2), (3, 4))}
     *
     * @param cols a list of columns on the left side of the filter.
     * @param rows a list of tuple values.
     * @return a tuple filter.
     */
    public static TupleFilter notIn(List<Column> cols, List<List<Object>> rows) {
        return Filter.tuple(cols).notIn(rows);
    }

    /**
     * Creates a composite filter with {@link io.cherlabs.sqm.core.CompositeFilter.Operator#And} op between the filters.
     *
     * @param filters a list of filters.
     * @return a composite filter.
     */
    public static CompositeFilter and(Filter... filters) {
        return Filter.and(filters);
    }

    /**
     * Creates a composite filter with {@link io.cherlabs.sqm.core.CompositeFilter.Operator#And} op between the filters.
     *
     * @param filters a list of filters.
     * @return a composite filter.
     */
    public static CompositeFilter and(List<Filter> filters) {
        return Filter.and(filters);
    }

    /**
     * Creates a composite filter with {@link io.cherlabs.sqm.core.CompositeFilter.Operator#Or} op between the filters.
     *
     * @param filters a list of filters.
     * @return a composite filter.
     */
    public static CompositeFilter or(Filter... filters) {
        return Filter.or(filters);
    }

    /**
     * Creates a composite filter with {@link io.cherlabs.sqm.core.CompositeFilter.Operator#And} op between the filters.
     *
     * @param filters a list of filters.
     * @return a composite filter.
     */
    public static CompositeFilter or(List<Filter> filters) {
        return Filter.or(filters);
    }

    /**
     * Creates a composite filter with {@link io.cherlabs.sqm.core.CompositeFilter.Operator#Not} op to negate the filter.
     *
     * @param filter a filter.
     * @return a composite filter.
     */
    public static CompositeFilter not(Filter filter) {
        return Filter.not(filter);
    }

    /* ========================= Joins ========================= */

    /**
     * Creates an inner join with the provided table.
     *
     * @param table a table to join with.
     * @return a table join.
     */
    public static TableJoin inner(Table table) {
        return Join.inner(table);
    }

    /**
     * Creates a left join with the provided table.
     *
     * @param table a table to join with.
     * @return a table join.
     */
    public static TableJoin left(Table table) {
        return Join.left(table);
    }

    /**
     * Creates a right join with the provided table.
     *
     * @param table a table to join with.
     * @return a table join.
     */
    public static TableJoin right(Table table) {
        return Join.right(table);
    }

    /**
     * Creates a full join with the provided table.
     *
     * @param table a table to join with.
     * @return a table join.
     */
    public static TableJoin full(Table table) {
        return Join.full(table);
    }

    /**
     * Creates a cross join with the provided table.
     *
     * @param table a table to join with.
     * @return a table join.
     */
    public static TableJoin cross(Table table) {
        return Join.cross(table);
    }

    /**
     * Adds ON statement represented by a filter to a join.
     *
     * @param j  a join.
     * @param on a filter.
     * @return a table join.
     */
    public static TableJoin on(TableJoin j, Filter on) {
        return j.on(on);
    }

    /**
     * Creates a join represented by a string expression.
     *
     * @param expression a string expression.
     * @return an expression join.
     */
    public static ExpressionJoin joinExpr(String expression) {
        return Join.expr(expression);
    }

    /* ========================= GROUP BY / ORDER BY ========================= */

    /**
     * Creates a group by item from the provided table name and column name.
     *
     * @param table the name of the column.
     * @param col   the name of the table.
     * @return a group by item.
     */
    public static Group group(String table, String col) {
        return Group.by(Column.of(col).from(table));
    }

    /**
     * Creates a group by item from the provided table name and column name.
     *
     * @param table the name of the column.
     * @param col   the name of the table.
     * @param alias the column alias.
     * @return a group by item.
     */
    public static Group group(String table, String col, String alias) {
        return Group.by(Column.of(col).from(table).as(alias));
    }

    /**
     * Creates a group by item from the provided column.
     * For example: {@code GroupBy c1, c2, c3}
     *
     * @param col a column to be used in a group by statement.
     * @return a group by item.
     */
    public static Group group(Column col) {
        return Group.by(col);
    }

    /**
     * Creates a group by item from the ordinal value.
     * For example: {@code GroupBy 1, 2, 3}
     *
     * @param ordinal a value.
     * @return a group by item.
     */
    public static Group group(int ordinal) {
        return Group.by(ordinal);
    }

    /**
     * Creates an order by item from the provided table name and column name.
     *
     * @param table the name of the table.
     * @param col   the name of the column.
     * @return an order by item.
     */
    public static Order order(String table, String col) {
        return Order.by(Column.of(col).from(table));
    }

    /**
     * Creates an order by item from the provided table name and column name.
     *
     * @param table the name of the table.
     * @param col   the name of the column.
     * @param alias the column alias.
     * @return an order by item.
     */
    public static Order order(String table, String col, String alias) {
        return Order.by(Column.of(col).from(table).as(alias));
    }

    /**
     * Creates an order by item from the provided column.
     * For example: {@code OrderBy c1, c2, c3}
     *
     * @param col a column
     * @return an order by item.
     */
    public static Order order(Column col) {
        return Order.by(col);
    }

    /**
     * Creates an ascending order by item from the provided column.
     * For example: {@code OrderBy c1 ASC, c2 ASC, c3 ASC}
     *
     * @param col a column
     * @return an order by item.
     */
    public static Order asc(Column col) {
        return Order.by(col).asc();
    }

    /**
     * Creates a descending order by item from the provided column.
     * For example: {@code OrderBy c1 DESC, c2 DESC, c3 DESC}
     *
     * @param col a column
     * @return an order by item.
     */
    public static Order desc(Column col) {
        return Order.by(col).desc();
    }

    /* ========================= Query ========================= */

    /**
     * Creates an empty query object.
     *
     * @return a query.
     */
    public static SelectQuery query() {
        return new SelectQuery();
    }

    /**
     * Creates a {@link SelectQuery} with the list of columns.
     *
     * @param columns a list of columns to select.
     * @return a query.
     */
    public static SelectQuery select(Column... columns) {
        return Query.select(columns);
    }

    /**
     * Creates a query that represents a WITH statement.
     *
     * @param ctes a list of CTE queries.
     * @return a WITH query.
     */
    public static WithQuery with(CteQuery... ctes) {
        return Query.with(ctes);
    }

    /**
     * Creates a query that represents a CTE statement.
     * <p>Example of the CTE statement inside the WITH:</p>
     * <pre>
     *     {@code
     *     TABLE1 AS (
     *        SELECT * FROM SCHEMA.TABLE1
     *     )
     *     }
     * </pre>
     *
     * @param name the name of the CTE statement (TABLE1 in the example).
     * @return a CTE query.
     */
    public static CteQuery cte(String name) {
        return Query.cte(name);
    }

    /**
     * Creates a composite query consisting of a list of sub queries and operators between them.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     (SELECT * FROM TABLE1)
     *     UNION
     *     (SELECT * FROM TABLE2)
     *     INTERSECT
     *     (SELECT * FROM TABLE3)
     *     }
     * </pre>
     *
     * @param terms a list of sub queries.
     * @param ops   a list of operators. See {@link CompositeQuery.Kind}
     * @return a composite query.
     */
    public static CompositeQuery composite(List<Query> terms, List<CompositeQuery.Op> ops) {
        return Query.composite(terms, ops);
    }
}
