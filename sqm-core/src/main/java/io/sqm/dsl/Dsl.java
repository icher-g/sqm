package io.sqm.dsl;

import io.sqm.core.*;

import java.util.List;

/**
 * Minimal, ergonomic, static-import friendly helpers to build the core model
 * without touching parsers. Keep names short and predictable.
 * <p>
 * Usage (static import io.sqm.dsl.DSL.*):
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
    public final static Object NULL = null;

    private Dsl() {
    }

    /* ========================= Tables ========================= */

    /**
     * Creates a table with the provided name.
     *
     * @param name a name of the table.
     * @return a table.
     */
    public static Table tbl(String name) {
        return Table.of(name);
    }

    /**
     * Creates a table with the provided schema and name.
     *
     * @param schema a table schema.
     * @param name   a table name.
     * @return a table.
     */
    public static Table tbl(String schema, String name) {
        return Table.of(name).inSchema(schema);
    }

    /**
     * Wraps a query as a table for use in FROM statement.
     *
     * @param query a query to wrap.
     * @return a table that wraps a query.
     */
    public static QueryTable tbl(Query query) {
        return Query.table(query);
    }

    /* ========================= Columns ========================= */

    /**
     * Creates a column with the provided name.
     *
     * @param name a column name.
     * @return a column.
     */
    public static ColumnExpr col(String name) {
        return ColumnExpr.of(name);
    }

    /**
     * Creates a column with the provided table and name.
     *
     * @param table a column table.
     * @param name  a column name.
     * @return a column.
     */
    public static ColumnExpr col(String table, String name) {
        return ColumnExpr.of(name).inTable(table);
    }

    /**
     * Creates a column that is represented by a sub query.
     *
     * @param subquery a sub query.
     * @return a query column.
     */
    public static QueryExpr col(Query subquery) {
        return QueryExpr.of(subquery);
    }

    /* ========================= Select Items ====================== */

    /**
     * Creates a select item from a column with the provided name.
     *
     * @param name a column name.
     * @return a column.
     */
    public static SelectItem sel(String name) {
        return ColumnExpr.of(name).toSelectItem();
    }

    /**
     * Creates a select item from a column with the provided table and name.
     *
     * @param table a column table.
     * @param name  a column name.
     * @return a column.
     */
    public static SelectItem sel(String table, String name) {
        return ColumnExpr.of(table, name).toSelectItem();
    }

    /**
     * Creates SELECT item from expression.
     *
     * @param expr an expression.
     * @return SELECT item.
     */
    public static SelectItem sel(Expression expr) {
        return SelectItem.expr(expr);
    }

    /**
     * Crates a '*' item for SELECT statement.
     *
     * @return select item.
     */
    public static StarSelectItem star() {
        return StarSelectItem.of();
    }

    /**
     * Crates a qualified '*' item for SELECT statement: t.*
     *
     * @return select item.
     */
    public static QualifiedStarSelectItem star(String qualifier) {
        return QualifiedStarSelectItem.of(qualifier);
    }

    /* ========================= Functions ========================= */

    /**
     * Creates a column that represents a function call.
     *
     * @param name a name of the function.
     * @param args an array of arguments for the function. An array can be empty if a function does not accept any arguments.
     * @return a function column.
     */
    public static FunctionExpr func(String name, FunctionExpr.Arg... args) {
        return Expression.func(name, args);
    }

    /**
     * Creates a column that represents a function call.
     *
     * @param name        a name of the function.
     * @param distinctArg indicates whether DISTINCT should be added before the list of arguments in the function call. {@code COUNT(DISTINCT t.id) AS c}
     * @param args        an array of arguments for the function. An array can be empty if a function does not accept any arguments.
     * @return a function column.
     */
    public static FunctionExpr func(String name, boolean distinctArg, FunctionExpr.Arg... args) {
        return Expression.func(name, distinctArg, args);
    }

    /**
     * Creates a function argument represented by a column.
     *
     * @param col a column to be passed to a function.
     * @return a function argument.
     */
    public static FunctionExpr.Arg arg(ColumnExpr col) {
        return Expression.funcArg(col);
    }

    /**
     * Creates a function argument represented by a literal.
     *
     * @param value a literal value.
     * @return a function argument.
     */
    public static FunctionExpr.Arg arg(Object value) {
        return Expression.funcArg(value);
    }

    /**
     * Creates a function argument represented by a nested function call.
     *
     * @param call a nested function call.
     * @return a function argument.
     */
    public static FunctionExpr.Arg arg(FunctionExpr call) {
        return Expression.funcArg(call);
    }

    /**
     * Creates a function argument represented by a start '*'.
     *
     * @return a function argument.
     */
    public static FunctionExpr.Arg starArg() {
        return Expression.starArg();
    }

    /* ========================= CASE ========================= */

    /**
     * Creates a column that represents a CASE statement.
     *
     * @param whens an array of WHEN...THEN groups.
     * @return a case column.
     */
    public static CaseExpr kase(WhenThen... whens) { // "case" is reserved in Java
        return CaseExpr.of(List.of(whens));
    }

    /**
     * Creates a WHEN...THEN statement used in the CASE.
     *
     * @param condition a condition used in the WHEN statement.
     * @param thenValue a value returned by the statement if the WHEN statement returns true.
     * @return a WhenThen object.
     */
    public static WhenThen when(Predicate condition, Expression thenValue) {
        return WhenThen.of(condition, thenValue);
    }

    /**
     * Creates a WHEN...THEN statement with only WHEN.
     *
     * @param condition a condition used in the WHEN statement.
     * @return a WhenThen object.
     */
    public static WhenThen when(Predicate condition) {
        return WhenThen.of(condition);
    }

    /* ========================= Filters (Column RHS values) ========================= */

    /**
     * Creates a single value.
     *
     * @param value a value.
     * @return Values that represents a single value.
     */
    public static LiteralExpr lit(Object value) {
        return Expression.literal(value);
    }

    /**
     * Creates a list of values. Can be used in an IN filter.
     *
     * @param values an array of values.
     * @return Values that represents a list of values.
     */
    public static RowExpr row(Object... values) {
        return Expression.row(values);
    }

    /**
     * Creates a list of values. Can be used in an IN filter.
     *
     * @param values an array of values.
     * @return Values that represents a list of values.
     */
    public static RowExpr row(Expression... values) {
        return Expression.row(List.of(values));
    }

    /**
     * Creates a list of tuples.
     * For example: {@code WHERE (c1, c2) IN ((1, 2), (3, 4))}
     *
     * @param rows a list of tuples.
     * @return Values that represents a list of tuples.
     */
    public static RowListExpr rows(List<List<Object>> rows) {
        return Expression.rows(rows.stream().map(Expression::row).toList());
    }

    /**
     * Creates a list of tuples.
     * For example: {@code WHERE (c1, c2) IN ((1, 2), (3, 4))}
     *
     * @param rows a list of tuples.
     * @return Values that represents a list of tuples.
     */
    public static RowListExpr rows(RowExpr... rows) {
        return Expression.rows(List.of(rows));
    }

    /**
     * Creates a sub query value.
     * For example: {@code WHERE c1 IN (SELECT ID FROM t)}
     *
     * @param q a sbu query.
     * @return Values that represents a sub query.
     */
    public static QueryExpr subquery(Query q) {
        return Expression.subquery(q);
    }

    /* ========================= Joins ========================= */

    /**
     * Creates an inner join with the provided table.
     *
     * @param table a table to join with.
     * @return a table join.
     */
    public static OnJoin inner(TableRef table) {
        return Join.join(table);
    }

    /**
     * Creates a left join with the provided table.
     *
     * @param table a table to join with.
     * @return a table join.
     */
    public static OnJoin left(TableRef table) {
        return Join.left(table);
    }

    /**
     * Creates a right join with the provided table.
     *
     * @param table a table to join with.
     * @return a table join.
     */
    public static OnJoin right(TableRef table) {
        return Join.right(table);
    }

    /**
     * Creates a full join with the provided table.
     *
     * @param table a table to join with.
     * @return a table join.
     */
    public static OnJoin full(TableRef table) {
        return Join.full(table);
    }

    /**
     * Creates a cross join with the provided table.
     *
     * @param table a table to join with.
     * @return a table join.
     */
    public static CrossJoin cross(TableRef table) {
        return Join.cross(table);
    }

    /**
     * Creates a using join with the provided table.
     *
     * @param table        a table to join.
     * @param usingColumns a list of columns to be used for joining.
     * @return A newly created instance of USING JOIN with the provided table and a list of columns.
     */
    public static UsingJoin cross(TableRef table, List<String> usingColumns) {
        return Join.using(table, usingColumns);
    }

    /**
     * Creates a natural join with the provided table.
     *
     * @param table a table to join with.
     * @return a table join.
     */
    public static NaturalJoin natural(TableRef table) {
        return Join.natural(table);
    }

    /* ========================= GROUP BY / ORDER BY ========================= */

    /**
     * Creates a group by item from the provided column name.
     *
     * @param col   the name of the table.
     * @return a group by item.
     */
    public static GroupItem group(String col) {
        return GroupItem.by(col(col));
    }

    /**
     * Creates a group by item from the provided table name and column name.
     *
     * @param table the name of the column.
     * @param col   the name of the table.
     * @return a group by item.
     */
    public static GroupItem group(String table, String col) {
        return GroupItem.by(col(table, col));
    }

    /**
     * Creates a group by item from the provided column.
     * For example: {@code GroupBy c1, c2, c3}
     *
     * @param col a column to be used in a group by statement.
     * @return a group by item.
     */
    public static GroupItem group(Expression col) {
        return GroupItem.by(col);
    }

    /**
     * Creates a group by item from the ordinal value.
     * For example: {@code GroupBy 1, 2, 3}
     *
     * @param ordinal a value.
     * @return a group by item.
     */
    public static GroupItem group(int ordinal) {
        return GroupItem.by(ordinal);
    }

    /**
     * Creates an order by item from the provided column name.
     *
     * @param col   the name of the column.
     * @return an order by item.
     */
    public static OrderItem order(String col) {
        return OrderItem.by(col(col));
    }

    /**
     * Creates an order by item from the provided table name and column name.
     *
     * @param table the name of the table.
     * @param col   the name of the column.
     * @return an order by item.
     */
    public static OrderItem order(String table, String col) {
        return OrderItem.by(col(table, col));
    }

    /**
     * Creates an order by item from the provided column.
     * For example: {@code OrderBy c1, c2, c3}
     *
     * @param col a column
     * @return an order by item.
     */
    public static OrderItem order(Expression col) {
        return OrderItem.by(col);
    }

    /* ========================= Query ========================= */

    /**
     * Creates a {@link SelectQuery} with the list of items.
     *
     * @param items a list of items to select.
     * @return a query.
     */
    public static SelectQuery select(SelectItem... items) {
        return Query.select(items);
    }

    /**
     * Creates a {@link SelectQuery} with the list of expressions.
     *
     * @param expressions a list of expressions to select.
     * @return a query.
     */
    public static SelectQuery select(Expression... expressions) {
        return Query.select(expressions);
    }

    /**
     * Creates a query that represents a WITH statement.
     *
     * @param ctes a list of CTE queries.
     * @return a WITH query.
     */
    public static WithQuery with(CteDef... ctes) {
        return Query.with(List.of(ctes), null);
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
    public static CteDef cte(String name) {
        return Query.cte(name);
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
     * @param body a sub query wrapped by the CTE.
     * @return a CTE query.
     */
    public static CteDef cte(String name, Query body) {
        return Query.cte(name, body);
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
     * @param ops   a list of operators. See {@link SetOperator}
     * @return a composite query.
     */
    public static CompositeQuery compose(List<Query> terms, List<SetOperator> ops) {
        return Query.compose(terms, ops);
    }
}
