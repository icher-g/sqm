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
public final class DSL {
    private DSL() {
    }

    /* ========================= Tables ========================= */

    /**
     * Creates a table with the provided name.
     *
     * @param name a name of the table.
     * @return a table.
     */
    public static NamedTable t(String name) {
        return Table.of(name);
    }

    /**
     * Creates a table with the provided schema and name.
     *
     * @param schema a table schema.
     * @param name   a table name.
     * @return a table.
     */
    public static NamedTable t(String schema, String name) {
        return Table.of(name).from(schema);
    }

    /**
     * Creates a table with the provided schema, name and alias.
     *
     * @param schema a table schema.
     * @param name   a table name.
     * @param alias  a table alias.
     * @return a table.
     */
    public static NamedTable tAs(String schema, String name, String alias) {
        return Table.of(name).from(schema).as(alias);
    }

    /**
     * Creates a table with the provided name and alias.
     *
     * @param name  a name of the table.
     * @param alias a table alias.
     * @return a table.
     */
    public static NamedTable tAs(String name, String alias) {
        return Table.of(name).as(alias);
    }

    /* ========================= Columns ========================= */

    /**
     * Creates a column with the provided name.
     *
     * @param name a column name.
     * @return a column.
     */
    public static NamedColumn c(String name) {
        return Column.of(name);
    }

    /**
     * Creates a column with the provided table and name.
     *
     * @param table a column table.
     * @param name  a column name.
     * @return a column.
     */
    public static NamedColumn c(String table, String name) {
        return Column.of(name).from(table);
    }

    /**
     * Creates a column with the provided name and alias.
     *
     * @param name  a column name.
     * @param alias a column alias.
     * @return a column.
     */
    public static NamedColumn cAs(String name, String alias) {
        return Column.of(name).as(alias);
    }

    /**
     * Creates a column with the provided table, name and alias.
     *
     * @param table a column table.
     * @param name  a column name.
     * @param alias a column alias.
     * @return a column.
     */
    public static NamedColumn cAs(String table, String name, String alias) {
        return Column.of(name).from(table).as(alias);
    }

    /**
     * Creates a column that is represented by a sub query.
     *
     * @param subquery a sub query.
     * @return a query column.
     */
    public static QueryColumn c(Query<?> subquery) {
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
    public static Values.Subquery subq(Query<?> q) {
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

    /* ========================= Filters (predicates) ========================= */

    /**
     * Creates a column filter.
     * For example: {@code WHERE c1 IN (SELECT ID FROM t)}
     *
     * @param col a column (c1) the filter will be applied on.
     * @return a column filter.
     */
    public static ColumnFilter f(Column col) {
        return Filter.column(col);
    }

    /**
     * Creates a tuple filter.
     * For example: {@code WHERE (c1, c2) IN ((1, 2), (3, 4))}
     *
     * @param cols a list of columns in a tuple. (c1, c2)
     * @return a tuple filter.
     */
    public static TupleFilter tf(List<Column> cols) {
        return Filter.tuple(cols);
    }

    /* ========================= column operators ========================= */

    /**
     * Creates column filter with {@link io.cherlabs.sqlmodel.core.ColumnFilter.Operator#Eq} operator and value.
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
     * Creates column filter with {@link io.cherlabs.sqlmodel.core.ColumnFilter.Operator#Eq} operator between two columns.
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
     * Creates column filter with {@link io.cherlabs.sqlmodel.core.ColumnFilter.Operator#Ne} operator and value.
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
     * Creates column filter with {@link io.cherlabs.sqlmodel.core.ColumnFilter.Operator#Ne} operator between two columns.
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
     * Creates column filter with {@link io.cherlabs.sqlmodel.core.ColumnFilter.Operator#Lt} operator and value.
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
     * Creates column filter with {@link io.cherlabs.sqlmodel.core.ColumnFilter.Operator#Lt} operator between two columns.
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
     * Creates column filter with {@link io.cherlabs.sqlmodel.core.ColumnFilter.Operator#Lte} operator and value.
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
     * Creates column filter with {@link io.cherlabs.sqlmodel.core.ColumnFilter.Operator#Lte} operator between two columns.
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
     * Creates column filter with {@link io.cherlabs.sqlmodel.core.ColumnFilter.Operator#Gt} operator and value.
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
     * Creates column filter with {@link io.cherlabs.sqlmodel.core.ColumnFilter.Operator#Gt} operator between two columns.
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
     * Creates column filter with {@link io.cherlabs.sqlmodel.core.ColumnFilter.Operator#Gte} operator and value.
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
     * Creates column filter with {@link io.cherlabs.sqlmodel.core.ColumnFilter.Operator#Gte} operator between two columns.
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
     * Creates column filter with {@link io.cherlabs.sqlmodel.core.ColumnFilter.Operator#In} operator and values.
     * For example: {@code WHERE c1 IN (1, 2, 3)}
     *
     * @param col    a column.
     * @param values a list of values.
     * @return a column filter.
     */
    public static ColumnFilter in(Column col, List<?> values) {
        return Filter.column(col).in(values.stream().map(Object.class::cast).collect(Collectors.toList()));
    }

    /**
     * Creates column filter with {@link io.cherlabs.sqlmodel.core.ColumnFilter.Operator#NotIn} operator and values.
     * For example: {@code WHERE c1 NOT IN (1, 2, 3)}
     *
     * @param col    a column.
     * @param values a list of values.
     * @return a column filter.
     */
    public static ColumnFilter notIn(Column col, List<?> values) {
        return Filter.column(col).notIn(values.stream().map(Object.class::cast).collect(Collectors.toList()));
    }

    /**
     * Creates column filter with {@link io.cherlabs.sqlmodel.core.ColumnFilter.Operator#Range} operator.
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
     * Creates a tuple filter with {@link io.cherlabs.sqlmodel.core.TupleFilter.Operator#In} operator.
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
     * Creates a tuple filter with {@link io.cherlabs.sqlmodel.core.TupleFilter.Operator#NotIn} operator.
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
     * Creates a composite filter with {@link io.cherlabs.sqlmodel.core.CompositeFilter.Operator#And} operator between the filters.
     *
     * @param filters a list of filters.
     * @return a composite filter.
     */
    public static CompositeFilter and(Filter... filters) {
        return Filter.and(filters);
    }

    /**
     * Creates a composite filter with {@link io.cherlabs.sqlmodel.core.CompositeFilter.Operator#And} operator between the filters.
     *
     * @param filters a list of filters.
     * @return a composite filter.
     */
    public static CompositeFilter and(List<Filter> filters) {
        return Filter.and(filters);
    }

    /**
     * Creates a composite filter with {@link io.cherlabs.sqlmodel.core.CompositeFilter.Operator#Or} operator between the filters.
     *
     * @param filters a list of filters.
     * @return a composite filter.
     */
    public static CompositeFilter or(Filter... filters) {
        return Filter.or(filters);
    }

    /**
     * Creates a composite filter with {@link io.cherlabs.sqlmodel.core.CompositeFilter.Operator#And} operator between the filters.
     *
     * @param filters a list of filters.
     * @return a composite filter.
     */
    public static CompositeFilter or(List<Filter> filters) {
        return Filter.or(filters);
    }

    /**
     * Creates a composite filter with {@link io.cherlabs.sqlmodel.core.CompositeFilter.Operator#Not} operator to negate the filter.
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
     * Creates a group by item from the provided column.
     * For example: {@code GroupBy c1, c2, c3}
     *
     * @param col a column to be used in a group by statement.
     * @return a group by item.
     */
    public static Group g(Column col) {
        return Group.by(col);
    }

    /**
     * Creates a group by item from the ordinal value.
     * For example: {@code GroupBy 1, 2, 3}
     *
     * @param ordinal a value.
     * @return a group by item.
     */
    public static Group g(int ordinal) {
        return Group.ofOrdinal(ordinal);
    }

    /**
     * Creates an order by item from the provided column.
     * For example: {@code OrderBy c1, c2, c3}
     *
     * @param col a column
     * @return an order by item.
     */
    public static Order o(Column col) {
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

    /**
     * Adds NULLS definition to an order by item.
     *
     * @param oi an order by item.
     * @param n  NULLS.
     * @return an order by item.
     */
    public static Order nulls(Order oi, Nulls n) {
        return new Order(oi.column(), oi.direction(), n, oi.collate());
    }

    /**
     * Adds collate definition to an order by item.
     *
     * @param oi     an order by item.
     * @param locale collate definition.
     * @return an order by item.
     */
    public static Order collate(Order oi, String locale) {
        return new Order(oi.column(), oi.direction(), oi.nulls(), locale);
    }

    /* ========================= Query ========================= */

    /**
     * Creates an empty query object.
     *
     * @return a query.
     */
    public static SelectQuery q() {
        return new SelectQuery();
    }

    /**
     * Create a query that represents a WITH statement.
     *
     * @param ctes a list of CTE queries.
     * @return a with query.
     */
    public static WithQuery with(Query<?>... ctes) {
        return new WithQuery(List.of(ctes));
    }

    /**
     * Adds columns to SELECT statement of the query.
     *
     * @param q    a query to add to.
     * @param cols a list of columns to be added.
     * @return an updated query.
     */
    public static Query<?> select(Query<?> q, Column... cols) {
        return q.select(List.of(cols));
    }

    /**
     * Adds columns to SELECT statement of the query.
     *
     * @param q    a query to add to.
     * @param cols a list of columns to be added.
     * @return an updated query.
     */
    public static Query<?> select(Query<?> q, List<Column> cols) {
        return q.select(cols);
    }

    /**
     * Sets a table as a FROM statement on a query.
     *
     * @param q     a query to update.
     * @param table a table to be used as a FROM statement.
     * @return an updated query.
     */
    public static Query<?> from(Query<?> q, Table table) {
        return q.from(table);
    }

    /**
     * Sets a filter as a WHERE statement on a query.
     *
     * @param q      a query to update.
     * @param filter a filter to be used as a WHERE statement.
     * @return an updated query.
     */
    public static Query<?> where(Query<?> q, Filter filter) {
        return q.where(filter);
    }

    /**
     * Sets a filter as a HAVING statement on a query.
     *
     * @param q      a query to update.
     * @param filter a filter to be used as a HAVING statement.
     * @return an updated query.
     */
    public static Query<?> having(Query<?> q, Filter filter) {
        return q.having(filter);
    }

    /**
     * Adds a list of joins to the query.
     *
     * @param q     a query to update.
     * @param joins a list of joins to be added.
     * @return an updated query.
     */
    public static Query<?> join(Query<?> q, Join... joins) {
        return q.join(List.of(joins));
    }

    /**
     * Adds a list of joins to the query.
     *
     * @param q     a query to update.
     * @param joins a list of joins to be added.
     * @return an updated query.
     */
    public static Query<?> join(Query<?> q, List<Join> joins) {
        return q.join(joins);
    }

    /**
     * Adds a list of group by items to the query.
     *
     * @param q     a query to update.
     * @param items a list of group by items to be added.
     * @return an updated query.
     */
    public static Query<?> groupBy(Query<?> q, Group... items) {
        return q.groupBy(List.of(items));
    }

    /**
     * Adds a list of group by items to the query.
     *
     * @param q     a query to update.
     * @param items a list of group by items to be added.
     * @return an updated query.
     */
    public static Query<?> groupBy(Query<?> q, List<Group> items) {
        return q.groupBy(items);
    }

    /**
     * Adds a list of order by items to the query.
     *
     * @param q     a query to update.
     * @param items a list of order by items to be added.
     * @return an updated query.
     */
    public static Query<?> orderBy(Query<?> q, Order... items) {
        return q.orderBy(List.of(items));
    }

    /**
     * Adds a list of order by items to the query.
     *
     * @param q     a query to update.
     * @param items a list of order by items to be added.
     * @return an updated query.
     */
    public static Query<?> orderBy(Query<?> q, List<Order> items) {
        return q.orderBy(items);
    }

    /**
     * Set distinct to true on a query.
     *
     * @param q a query to update.
     * @return an updated query.
     */
    public static Query<?> distinct(Query<?> q) {
        return q.distinct(true);
    }

    /**
     * Sets a limit on a query.
     *
     * @param q     a query to update.
     * @param limit a limit to set.
     * @return an updated query.
     */
    public static Query<?> limit(Query<?> q, long limit) {
        return q.limit(limit);
    }

    /**
     * Sets an offset on a query.
     *
     * @param q      a query to update.
     * @param offset an offset to set.
     * @return an updated query.
     */
    public static Query<?> offset(Query<?> q, long offset) {
        return q.offset(offset);
    }
}
