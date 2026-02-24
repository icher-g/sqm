package io.sqm.dsl;

import io.sqm.core.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal, ergonomic, static-import friendly helpers to build the core model
 * without touching parsers. Keep names short and predictable.
 * <p>
 * Usage (static import io.sqm.dsl.DSL.*):
 * </p>
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
    /**
     * Defines a NULL object.
     */
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
        return tbl(null, name, null, Table.Inheritance.DEFAULT);
    }

    /**
     * Creates a table with the provided schema and name.
     *
     * @param schema a table schema.
     * @param name   a table name.
     * @return a table.
     */
    public static Table tbl(String schema, String name) {
        return tbl(schema, name, null, Table.Inheritance.DEFAULT);
    }

    /**
     * Creates a table with the provided quote-aware table name identifier.
     *
     * @param name table name identifier
     * @return a table
     */
    public static Table tbl(Identifier name) {
        return Table.of(name);
    }

    /**
     * Creates a table with the provided quote-aware schema and name identifiers.
     *
     * @param schema schema identifier
     * @param name table name identifier
     * @return a table
     */
    public static Table tbl(Identifier schema, Identifier name) {
        return Table.of(schema, name, null, Table.Inheritance.DEFAULT);
    }

    /**
     * Creates a table with provided schema, name and alias.
     *
     * @param schema optional schema identifier
     * @param name   table name identifier (unqualified)
     * @param alias  optional table alias identifier
     * @return a newly created table instance
     */
    static Table tbl(String schema, String name, String alias) {
        return tbl(schema, name, alias, Table.Inheritance.DEFAULT);
    }

    /**
     * Creates a table with provided parameters.
     *
     * @param schema      optional schema identifier
     * @param name        table name identifier (unqualified)
     * @param alias       optional table alias identifier
     * @param inheritance inheritance behavior
     * @return a newly created table instance
     */
    static Table tbl(String schema, String name, String alias, Table.Inheritance inheritance) {
        return Table.of(
            schema == null ? null : Identifier.of(schema),
            Identifier.of(name),
            alias == null ? null : Identifier.of(alias),
            inheritance
        );
    }

    /**
     * Wraps a query as a table for use in FROM statement.
     *
     * @param query a query to wrap.
     * @return a table that wraps a query.
     */
    public static QueryTable tbl(Query query) {
        return TableRef.query(query);
    }

    /**
     * Wraps a list of rows expressions as a table for use in FROM statement.
     *
     * @param expr a list of rows expressions to wrap.
     * @return a table that wraps a list of rows expressions.
     */
    public static ValuesTable tbl(RowListExpr expr) {
        return TableRef.values(expr);
    }

    /**
     * Wraps a function expression as a table for use in FROM statement.
     *
     * @param expr a function expression to wrap.
     * @return a table that wraps a function expression.
     */
    public static FunctionTable tbl(FunctionExpr expr) {
        return TableRef.function(expr);
    }

    /* ========================= Columns ========================= */

    /**
     * Creates a column with the provided name.
     *
     * @param name a column name.
     * @return a column.
     */
    public static ColumnExpr col(String name) {
        return ColumnExpr.of(null, Identifier.of(name));
    }

    /**
     * Creates a column with the provided table and name.
     *
     * @param table a column table.
     * @param name  a column name.
     * @return a column.
     */
    public static ColumnExpr col(String table, String name) {
        return ColumnExpr.of(
            table == null ? null : Identifier.of(table),
            Identifier.of(name)
        );
    }

    /**
     * Creates a column with the provided quote-aware column name identifier.
     *
     * @param name column name identifier
     * @return a column
     */
    public static ColumnExpr col(Identifier name) {
        return ColumnExpr.of(null, name);
    }

    /**
     * Creates a column with the provided quote-aware table alias and column name identifiers.
     *
     * @param table table alias identifier
     * @param name column name identifier
     * @return a column
     */
    public static ColumnExpr col(Identifier table, Identifier name) {
        return ColumnExpr.of(table, name);
    }

    /* ========================= Select Items ====================== */

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
     * @param qualifier a star qualifier.
     * @return select item.
     */
    public static QualifiedStarSelectItem star(String qualifier) {
        return QualifiedStarSelectItem.of(Identifier.of(qualifier));
    }

    /**
     * Creates a qualified '*' item for SELECT statement with a quote-aware qualifier: t.*
     *
     * @param qualifier quote-aware qualifier identifier
     * @return select item
     */
    public static QualifiedStarSelectItem star(Identifier qualifier) {
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
        return FunctionExpr.of(QualifiedName.of(name.split("\\.")), List.of(args), null, null, null, null);
    }

    /**
     * Creates a bare operator name (for example {@code +}, {@code ->}, {@code @>}).
     *
     * @param symbol operator symbol token
     * @return operator name
     */
    public static OperatorName op(String symbol) {
        return OperatorName.of(symbol);
    }

    /**
     * Creates an {@code OPERATOR(schema.symbol)} operator name using a schema value.
     *
     * @param schema operator schema
     * @param symbol operator symbol token
     * @return operator name using {@code OPERATOR(...)} syntax
     */
    public static OperatorName op(String schema, String symbol) {
        return OperatorName.operator(QualifiedName.of(schema), symbol);
    }

    /**
     * Creates an {@code OPERATOR(schema.symbol)} operator name using a quote-aware schema path.
     *
     * @param schema operator schema path
     * @param symbol operator symbol token
     * @return operator name using {@code OPERATOR(...)} syntax
     */
    public static OperatorName op(QualifiedName schema, String symbol) {
        return OperatorName.operator(schema, symbol);
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
     * Creates a function argument represented by an expression.
     *
     * @param expr an expression.
     * @return a function argument.
     */
    public static FunctionExpr.Arg arg(Expression expr) {
        return Expression.funcArg(expr);
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

    /* ========================= Parameters ========================= */

    /**
     * Creates a new anonymous positional parameter with the given position.
     *
     * @return an anonymous positional parameter
     */
    public static AnonymousParamExpr param() {
        return AnonymousParamExpr.of();
    }

    /**
     * Creates a new ordinal parameter with the given index.
     *
     * @param index 1-based index of the parameter
     * @return a new ordinal parameter expression
     */
    public static OrdinalParamExpr param(int index) {
        return OrdinalParamExpr.of(index);
    }

    /**
     * Creates a new named parameter with the given canonical name.
     *
     * @param name parameter name without prefix
     * @return a named parameter expression
     */
    public static NamedParamExpr param(String name) {
        return NamedParamExpr.of(name);
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
        return WhenThen.when(condition);
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
        return Expression.rows(rows
            .stream()
            .map(Expression::row)
            .toList());
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

    /* ========================= Arrays ========================= */

    /**
     * Creates an array constructor expression.
     *
     * @param elements array elements
     * @return array constructor expression
     */
    public static ArrayExpr array(Expression... elements) {
        return ArrayExpr.of(elements);
    }

    /**
     * Creates an array constructor expression.
     *
     * @param elements array elements
     * @return array constructor expression
     */
    public static ArrayExpr array(List<? extends Expression> elements) {
        return ArrayExpr.of(List.copyOf(elements));
    }

    /* ========================= TypeName ========================= */

    /**
     * Convenience factory for a qualified type name like {@code schema.type} or {@code int4}.
     *
     * @param parts identifier parts (dot-joined)
     * @return a qualified {@link TypeName}
     */
    public static TypeName type(String... parts) {
        return TypeName.of(QualifiedName.of(List.of(parts)), null, List.of(), 0, TimeZoneSpec.NONE);
    }

    /**
     * Convenience factory for a qualified type name using a quote-aware qualified path.
     *
     * @param qualifiedName quote-aware qualified type name
     * @return a qualified {@link TypeName}
     */
    public static TypeName type(QualifiedName qualifiedName) {
        return TypeName.of(qualifiedName, null, List.of(), 0, TimeZoneSpec.NONE);
    }

    /**
     * Convenience factory for a keyword type name like {@code double precision}.
     *
     * @param keyword keyword tokens (space-joined)
     * @return a keyword-based {@link TypeName}
     */
    public static TypeName type(TypeKeyword keyword) {
        return TypeName.of(null, keyword, List.of(), 0, TimeZoneSpec.NONE);
    }

    /**
     * Creates a cast expression.
     *
     * @param expr expression to cast
     * @param type target type
     * @return a cast expression
     */
    public static CastExpr cast(Expression expr, TypeName type) {
        return CastExpr.of(expr, type);
    }

    /* ========================= Joins ========================= */

    /**
     * Creates an inner join with the provided table.
     *
     * @param table a table to join with.
     * @return a table join.
     */
    public static OnJoin inner(TableRef table) {
        return Join.inner(table);
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
     * @param col the name of the table.
     * @return a group by item.
     */
    public static GroupItem group(String col) {
        return GroupItem.of(col(col));
    }

    /**
     * Creates a group by item from the provided table name and column name.
     *
     * @param table the name of the column.
     * @param col   the name of the table.
     * @return a group by item.
     */
    public static GroupItem group(String table, String col) {
        return GroupItem.of(col(table, col));
    }

    /**
     * Creates a group by item from the provided column.
     * For example: {@code GroupBy c1, c2, c3}
     *
     * @param col a column to be used in a group by statement.
     * @return a group by item.
     */
    public static GroupItem group(Expression col) {
        return GroupItem.of(col);
    }

    /**
     * Creates a group by item from the ordinal value.
     * For example: {@code GroupBy 1, 2, 3}
     *
     * @param ordinal a value.
     * @return a group by item.
     */
    public static GroupItem group(int ordinal) {
        return GroupItem.of(ordinal);
    }

    /**
     * Creates a grouping set item, for example {@code (a, b)} or {@code ()}.
     *
     * @param items grouping items inside the set
     * @return a grouping set item
     */
    public static GroupItem groupingSet(GroupItem... items) {
        return GroupItem.groupingSet(items);
    }

    /**
     * Creates a {@code GROUPING SETS (...)} item.
     *
     * @param sets grouping set elements
     * @return a grouping sets item
     */
    public static GroupItem groupingSets(GroupItem... sets) {
        return GroupItem.groupingSets(sets);
    }

    /**
     * Creates a {@code ROLLUP (...)} item.
     *
     * @param items grouping items inside the rollup
     * @return a rollup item
     */
    public static GroupItem rollup(GroupItem... items) {
        return GroupItem.rollup(items);
    }

    /**
     * Creates a {@code CUBE (...)} item.
     *
     * @param items grouping items inside the cube
     * @return a cube item
     */
    public static GroupItem cube(GroupItem... items) {
        return GroupItem.cube(items);
    }

    /**
     * Creates an order by item from the provided column name.
     *
     * @param col the name of the column.
     * @return an order by item.
     */
    public static OrderItem order(String col) {
        return OrderItem.of(col(col));
    }

    /**
     * Creates an order by item from the provided table name and column name.
     *
     * @param table the name of the table.
     * @param col   the name of the column.
     * @return an order by item.
     */
    public static OrderItem order(String table, String col) {
        return OrderItem.of(col(table, col));
    }

    /**
     * Creates an order by item from the provided column.
     * For example: {@code OrderBy c1, c2, c3}
     *
     * @param col a column
     * @return an order by item.
     */
    public static OrderItem order(Expression col) {
        return OrderItem.of(col);
    }

    /**
     * Creates an order by item from the ordinal value.
     * For example: {@code ORDER BY 1, 2, 3}
     *
     * @param ordinal an ordinal value.
     * @return an order by item.
     */
    public static OrderItem order(int ordinal) {
        return OrderItem.of(ordinal);
    }

    /**
     * Creates OrderBy statement from the list of provided items.
     *
     * @param items a list of OrderBy items.
     * @return an OrderBy statement.
     */
    public static OrderBy orderBy(OrderItem... items) {
        return OrderBy.of(List.of(items));
    }

    /* ========================= WINDOW clause (named windows on SELECT) ========================= */

    /**
     * Creates a named window definition in the {@code WINDOW} clause.
     * <p>Example SQL:</p>
     * <pre>
     * WINDOW w AS (PARTITION BY dept ORDER BY salary)
     * </pre>
     *
     * @param name the window name
     * @param spec the underlying window specification
     * @return a new {@link WindowDef}
     */
    public static WindowDef window(String name, OverSpec.Def spec) {
        return WindowDef.of(Identifier.of(name), spec);
    }

    /**
     * Creates a named window definition with {@code PARTITION BY}.
     * <p>Example SQL:</p>
     * <pre>
     * WINDOW w AS (PARTITION BY dept)
     * </pre>
     *
     * @param name        the window name
     * @param partitionBy the partition-by specification
     * @return a new {@link WindowDef}
     */
    public static WindowDef window(String name, PartitionBy partitionBy) {
        return WindowDef.of(Identifier.of(name), OverSpec.def(partitionBy, null, null, null));
    }

    /**
     * Creates a named window definition with {@code PARTITION BY} and {@code ORDER BY}.
     * <p>Example SQL:</p>
     * <pre>
     * WINDOW w AS (PARTITION BY dept ORDER BY salary DESC)
     * </pre>
     *
     * @param name        the window name
     * @param partitionBy the partition-by specification
     * @param orderBy     the order-by specification
     * @return a new {@link WindowDef}
     */
    public static WindowDef window(String name, PartitionBy partitionBy, OrderBy orderBy) {
        return WindowDef.of(Identifier.of(name), OverSpec.def(partitionBy, orderBy, null, null));
    }

    /**
     * Creates a named window definition with a frame specification.
     * <p>Example SQL:</p>
     * <pre>
     * WINDOW w AS (PARTITION BY dept ORDER BY ts ROWS 5 PRECEDING)
     * </pre>
     *
     * @param name        the window name
     * @param partitionBy the partition-by specification
     * @param orderBy     the order-by specification
     * @param frame       the frame specification
     * @return a new {@link WindowDef}
     */
    public static WindowDef window(String name, PartitionBy partitionBy, OrderBy orderBy, FrameSpec frame) {
        return WindowDef.of(Identifier.of(name), OverSpec.def(partitionBy, orderBy, frame, null));
    }

    /**
     * Creates a named window definition with a frame and an exclusion clause.
     * <p>Example SQL:</p>
     * <pre>
     * WINDOW w AS (PARTITION BY dept ORDER BY ts ROWS BETWEEN 1 PRECEDING AND 1 FOLLOWING EXCLUDE TIES)
     * </pre>
     *
     * @param name        the window name
     * @param partitionBy the partition-by specification
     * @param orderBy     the order-by specification
     * @param frame       the frame specification
     * @param exclude     the exclusion clause
     * @return a new {@link WindowDef}
     */
    public static WindowDef window(String name, PartitionBy partitionBy, OrderBy orderBy, FrameSpec frame, OverSpec.Exclude exclude) {
        return WindowDef.of(Identifier.of(name), OverSpec.def(partitionBy, orderBy, frame, exclude));
    }

    /* ========================= OVER (attach to FunctionExpr) ========================= */

    /**
     * References a named window from the {@code WINDOW} clause.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(salary) OVER w
     * </pre>
     *
     * @param windowName the name of the referenced window
     * @return an {@link OverSpec.Ref}
     */
    public static OverSpec.Ref over(String windowName) {
        return OverSpec.ref(Identifier.of(windowName));
    }

    /**
     * Creates an empty inline {@code OVER()} specification.
     *
     * @return an {@link OverSpec.Def}
     */
    public static OverSpec.Def over() {
        return OverSpec.def((Identifier) null, null, null, null);
    }

    /**
     * Creates an inline {@code OVER(...)} specification with {@code PARTITION BY}.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(amount) OVER (PARTITION BY acct)
     * </pre>
     *
     * @param partitionBy the partition-by specification
     * @return an {@link OverSpec.Def}
     */
    public static OverSpec.Def over(PartitionBy partitionBy) {
        return OverSpec.def(partitionBy, null, null, null);
    }

    /**
     * Creates an inline {@code OVER(...)} specification with {@code PARTITION BY} and {@code ORDER BY}.
     * <p>Example SQL:</p>
     * <pre>
     * RANK() OVER (PARTITION BY dept ORDER BY salary DESC)
     * </pre>
     *
     * @param partitionBy the partition-by specification
     * @param orderBy     the order-by specification
     * @return an {@link OverSpec.Def}
     */
    public static OverSpec.Def over(PartitionBy partitionBy, OrderBy orderBy) {
        return OverSpec.def(partitionBy, orderBy, null, null);
    }

    /**
     * Creates an inline {@code OVER(...)} specification including a window frame.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(amount) OVER (PARTITION BY acct ORDER BY ts ROWS 5 PRECEDING)
     * </pre>
     *
     * @param partitionBy the partition-by specification
     * @param orderBy     the order-by specification
     * @param frame       the frame specification
     * @return an {@link OverSpec.Def}
     */
    public static OverSpec.Def over(PartitionBy partitionBy, OrderBy orderBy, FrameSpec frame) {
        return OverSpec.def(partitionBy, orderBy, frame, null);
    }

    /**
     * Creates an inline {@code OVER(...)} specification including a window frame.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(amount) OVER (PARTITION BY acct ORDER BY ts ROWS 5 PRECEDING)
     * </pre>
     *
     * @param partitionBy the partition-by specification
     * @param frame       the frame specification
     * @return an {@link OverSpec.Def}
     */
    public static OverSpec.Def over(PartitionBy partitionBy, FrameSpec frame) {
        return OverSpec.def(partitionBy, null, frame, null);
    }

    /**
     * Creates an inline {@code OVER(...)} specification including a frame and an exclusion clause.
     * <p>Example SQL:</p>
     * <pre>
     * RANK() OVER (PARTITION BY grp ORDER BY score DESC GROUPS BETWEEN 1 PRECEDING AND 1 FOLLOWING EXCLUDE TIES)
     * </pre>
     *
     * @param partitionBy the partition-by specification
     * @param orderBy     the order-by specification
     * @param frame       the frame specification
     * @param exclude     the exclusion clause
     * @return an {@link OverSpec.Def}
     */
    public static OverSpec.Def over(PartitionBy partitionBy, OrderBy orderBy, FrameSpec frame, OverSpec.Exclude exclude) {
        return OverSpec.def(partitionBy, orderBy, frame, exclude);
    }

    /**
     * Creates an {@code OVER(...)} specification extending a base window name.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(salary) OVER (w ORDER BY ts)
     * </pre>
     *
     * @param baseWindow the referenced base window name
     * @param orderBy    an additional order-by clause
     * @return an {@link OverSpec.Def}
     */
    public static OverSpec.Def over(String baseWindow, OrderBy orderBy) {
        return OverSpec.def(Identifier.of(baseWindow), orderBy, null, null);
    }

    /**
     * Creates an {@code OVER(...)} definition extending only a base window name.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(salary) OVER (w)
     * </pre>
     *
     * @param baseWindow the referenced base window name
     * @return an {@link OverSpec.Def}
     */
    public static OverSpec.Def overDef(String baseWindow) {
        return OverSpec.def(Identifier.of(baseWindow), null, null, null);
    }

    /**
     * Creates an {@code OVER(...)} specification extending a base window name with a frame.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(amount) OVER (w ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
     * </pre>
     *
     * @param baseWindow the referenced base window name
     * @param orderBy    an optional order-by clause
     * @param frame      the frame specification
     * @return an {@link OverSpec.Def}
     */
    public static OverSpec.Def over(String baseWindow, OrderBy orderBy, FrameSpec frame) {
        return OverSpec.def(Identifier.of(baseWindow), orderBy, frame, null);
    }

    /**
     * Creates an {@code OVER(...)} specification extending a base window name with a frame.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(amount) OVER (w ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
     * </pre>
     *
     * @param baseWindow the referenced base window name
     * @param frame      the frame specification
     * @return an {@link OverSpec.Def}
     */
    public static OverSpec.Def over(String baseWindow, FrameSpec frame) {
        return OverSpec.def(Identifier.of(baseWindow), null, frame, null);
    }

    /**
     * Creates an {@code OVER(...)} specification extending a base window with a frame and exclusion.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(amount) OVER (w ROWS BETWEEN 2 PRECEDING AND 2 FOLLOWING EXCLUDE CURRENT ROW)
     * </pre>
     *
     * @param baseWindow the base window name
     * @param orderBy    an optional order-by clause
     * @param frame      the frame specification
     * @param exclude    the exclusion clause
     * @return an {@link OverSpec.Def}
     */
    public static OverSpec.Def over(String baseWindow, OrderBy orderBy, FrameSpec frame, OverSpec.Exclude exclude) {
        return OverSpec.def(Identifier.of(baseWindow), orderBy, frame, exclude);
    }

    /**
     * Creates an inline {@code OVER(...)} specification with {@code ORDER BY}.
     * <p>Example SQL:</p>
     * <pre>
     * ROW_NUMBER() OVER (ORDER BY created_at DESC)
     * </pre>
     *
     * @param orderBy the order-by specification
     * @return an {@link OverSpec.Def}
     */
    public static OverSpec.Def over(OrderBy orderBy) {
        return OverSpec.def((Identifier) null, orderBy, null, null);
    }

    /**
     * Creates an inline {@code OVER(...)} specification with {@code ORDER BY} and frame.
     *
     * @param orderBy the order-by specification
     * @param frame   the frame specification
     * @return an {@link OverSpec.Def}
     */
    public static OverSpec.Def over(OrderBy orderBy, FrameSpec frame) {
        return OverSpec.def((Identifier) null, orderBy, frame, null);
    }

    /**
     * Creates an inline {@code OVER(...)} specification with {@code ORDER BY}, frame and exclusion.
     *
     * @param orderBy the order-by specification
     * @param frame   the frame specification
     * @param exclude the exclusion clause
     * @return an {@link OverSpec.Def}
     */
    public static OverSpec.Def over(OrderBy orderBy, FrameSpec frame, OverSpec.Exclude exclude) {
        return OverSpec.def((Identifier) null, orderBy, frame, exclude);
    }

    /**
     * Creates an inline {@code OVER(...)} specification with frame.
     *
     * @param frame the frame specification
     * @return an {@link OverSpec.Def}
     */
    public static OverSpec.Def over(FrameSpec frame) {
        return OverSpec.def((Identifier) null, null, frame, null);
    }

    /**
     * Creates an inline {@code OVER(...)} specification with frame and exclusion.
     *
     * @param frame   the frame specification
     * @param exclude the exclusion clause
     * @return an {@link OverSpec.Def}
     */
    public static OverSpec.Def over(FrameSpec frame, OverSpec.Exclude exclude) {
        return OverSpec.def((Identifier) null, null, frame, exclude);
    }

    /* ========================= PARTITION BY ========================= */

    /**
     * Creates a {@code PARTITION BY} clause listing one or more expressions.
     * <p>Example SQL:</p>
     * <pre>
     * PARTITION BY dept, region
     * </pre>
     *
     * @param items expressions to partition by
     * @return a {@link PartitionBy} object
     */
    public static PartitionBy partition(Expression... items) {
        return PartitionBy.of(items);
    }

    /* ========================= FRAME: Single ========================= */

    /**
     * Creates a single-bound frame with the {@code ROWS} unit.
     * <p>Example SQL:</p>
     * <pre>
     * ROWS 5 PRECEDING
     * </pre>
     *
     * @param b the frame bound
     * @return a {@link FrameSpec} instance
     */
    public static FrameSpec rows(BoundSpec b) {
        return FrameSpec.single(FrameSpec.Unit.ROWS, b);
    }

    /**
     * Creates a single-bound frame with the {@code RANGE} unit.
     * <p>Example SQL:</p>
     * <pre>
     * RANGE UNBOUNDED PRECEDING
     * </pre>
     *
     * @param b the frame bound
     * @return a {@link FrameSpec} instance
     */
    public static FrameSpec range(BoundSpec b) {
        return FrameSpec.single(FrameSpec.Unit.RANGE, b);
    }

    /**
     * Creates a single-bound frame with the {@code GROUPS} unit.
     * <p>Example SQL:</p>
     * <pre>
     * GROUPS CURRENT ROW
     * </pre>
     *
     * @param b the frame bound
     * @return a {@link FrameSpec} instance
     */
    public static FrameSpec groups(BoundSpec b) {
        return FrameSpec.single(FrameSpec.Unit.GROUPS, b);
    }

    /* ========================= FRAME: Between ========================= */

    /**
     * Creates a two-bound frame with {@code ROWS BETWEEN ... AND ...}.
     * <p>Example SQL:</p>
     * <pre>
     * ROWS BETWEEN 2 PRECEDING AND CURRENT ROW
     * </pre>
     *
     * @param start the start bound
     * @param end   the end bound
     * @return a {@link FrameSpec} instance
     */
    public static FrameSpec rows(BoundSpec start, BoundSpec end) {
        return FrameSpec.between(FrameSpec.Unit.ROWS, start, end);
    }

    /**
     * Creates a two-bound frame with {@code RANGE BETWEEN ... AND ...}.
     * <p>Example SQL:</p>
     * <pre>
     * RANGE BETWEEN UNBOUNDED PRECEDING AND 3 FOLLOWING
     * </pre>
     *
     * @param start the start bound
     * @param end   the end bound
     * @return a {@link FrameSpec} instance
     */
    public static FrameSpec range(BoundSpec start, BoundSpec end) {
        return FrameSpec.between(FrameSpec.Unit.RANGE, start, end);
    }

    /**
     * Creates a two-bound frame with {@code GROUPS BETWEEN ... AND ...}.
     * <p>Example SQL:</p>
     * <pre>
     * GROUPS BETWEEN 1 PRECEDING AND 1 FOLLOWING
     * </pre>
     *
     * @param start the start bound
     * @param end   the end bound
     * @return a {@link FrameSpec} instance
     */
    public static FrameSpec groups(BoundSpec start, BoundSpec end) {
        return FrameSpec.between(FrameSpec.Unit.GROUPS, start, end);
    }

    /* ========================= BOUNDS ========================= */

    /**
     * Creates an {@code UNBOUNDED PRECEDING} bound.
     * <p>Example SQL:</p>
     * <pre>
     * UNBOUNDED PRECEDING
     * </pre>
     *
     * @return a {@link BoundSpec.UnboundedPreceding} instance
     */
    public static BoundSpec unboundedPreceding() {
        return BoundSpec.unboundedPreceding();
    }

    /**
     * Creates an {@code n PRECEDING} bound.
     * <p>Example SQL:</p>
     * <pre>
     * 5 PRECEDING
     * </pre>
     *
     * @param n number of preceding rows
     * @return a {@link BoundSpec.Preceding} instance
     */
    public static BoundSpec preceding(int n) {
        return BoundSpec.preceding(lit(n));
    }

    /**
     * Creates an {@code expr PRECEDING} bound.
     *
     * @param expr bound expression
     * @return a {@link BoundSpec.Preceding} instance
     */
    public static BoundSpec preceding(Expression expr) {
        return BoundSpec.preceding(expr);
    }

    /**
     * Creates a {@code CURRENT ROW} bound.
     * <p>Example SQL:</p>
     * <pre>
     * CURRENT ROW
     * </pre>
     *
     * @return a {@link BoundSpec.CurrentRow} instance
     */
    public static BoundSpec currentRow() {
        return BoundSpec.currentRow();
    }

    /**
     * Creates an {@code n FOLLOWING} bound.
     * <p>Example SQL:</p>
     * <pre>
     * 3 FOLLOWING
     * </pre>
     *
     * @param n number of following rows
     * @return a {@link BoundSpec.Following} instance
     */
    public static BoundSpec following(int n) {
        return BoundSpec.following(lit(n));
    }

    /**
     * Creates an {@code expr FOLLOWING} bound.
     *
     * @param expr bound expression
     * @return a {@link BoundSpec.Following} instance
     */
    public static BoundSpec following(Expression expr) {
        return BoundSpec.following(expr);
    }

    /**
     * Creates an {@code UNBOUNDED FOLLOWING} bound.
     * <p>Example SQL:</p>
     * <pre>
     * UNBOUNDED FOLLOWING
     * </pre>
     *
     * @return a {@link BoundSpec.UnboundedFollowing} instance
     */
    public static BoundSpec unboundedFollowing() {
        return BoundSpec.unboundedFollowing();
    }

    /* ========================= EXCLUDE ========================= */

    /**
     * Represents the {@code EXCLUDE CURRENT ROW} clause in a window frame.
     * <p>Example SQL:</p>
     * <pre>
     * EXCLUDE CURRENT ROW
     * </pre>
     *
     * @return {@link OverSpec.Exclude#CURRENT_ROW}
     */
    public static OverSpec.Exclude excludeCurrentRow() {
        return OverSpec.Exclude.CURRENT_ROW;
    }

    /**
     * Represents the {@code EXCLUDE GROUP} clause in a window frame.
     * <p>Example SQL:</p>
     * <pre>
     * EXCLUDE GROUP
     * </pre>
     *
     * @return {@link OverSpec.Exclude#GROUP}
     */
    public static OverSpec.Exclude excludeGroup() {
        return OverSpec.Exclude.GROUP;
    }

    /**
     * Represents the {@code EXCLUDE TIES} clause in a window frame.
     * <p>Example SQL:</p>
     * <pre>
     * EXCLUDE TIES
     * </pre>
     *
     * @return {@link OverSpec.Exclude#TIES}
     */
    public static OverSpec.Exclude excludeTies() {
        return OverSpec.Exclude.TIES;
    }

    /**
     * Represents the {@code EXCLUDE NO OTHERS} clause in a window frame.
     * <p>Example SQL:</p>
     * <pre>
     * EXCLUDE NO OTHERS
     * </pre>
     *
     * @return {@link OverSpec.Exclude#NO_OTHERS}
     */
    public static OverSpec.Exclude excludeNoOthers() {
        return OverSpec.Exclude.NO_OTHERS;
    }


    /* ========================= Predicate ========================= */

    /**
     * Creates a negate predicate for the provided predicate.
     *
     * @param predicate a predicate to negate.
     * @return A newly created instance of a predicate.
     */
    public static NotPredicate not(Predicate predicate) {
        return Predicate.not(predicate);
    }

    /**
     * Creates a unary predicate.
     *
     * @param expr a boolean expression: TRUE, FALSE or a boolean column.
     * @return a new instance of the unary predicate.
     */
    public static UnaryPredicate unary(Expression expr) {
        return Predicate.unary(expr);
    }

    /**
     * Creates EXISTS predicate.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     SELECT *
     *     FROM customers c
     *     WHERE EXISTS (
     *         SELECT 1
     *         FROM orders o
     *         WHERE o.customer_id = c.id
     *     );
     *     }
     * </pre>
     *
     * @param subquery a sub query which result to check.
     * @return A newly created EXISTS predicate.
     */
    public static ExistsPredicate exists(Query subquery) {
        return Predicate.exists(subquery);
    }

    /**
     * Creates NOT EXISTS predicate.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     SELECT *
     *     FROM customers c
     *     WHERE NOT EXISTS (
     *         SELECT 1
     *         FROM orders o
     *         WHERE o.customer_id = c.id
     *     );
     *     }
     * </pre>
     *
     * @param subquery a sub query which result to check.
     * @return A newly created NOT EXISTS predicate.
     */
    public static ExistsPredicate notExists(Query subquery) {
        return Predicate.notExists(subquery);
    }

    /* ========================= Query ========================= */

    /**
     * Creates a sub query value.
     * For example: {@code WHERE c1 IN (SELECT ID FROM t)}
     *
     * @param subquery a sbu query.
     * @return Values that represents a sub query.
     */
    public static QueryExpr expr(Query subquery) {
        return QueryExpr.of(subquery);
    }

    /**
     * Creates a {@link SelectQuery} with the list of expressions.
     *
     * @param nodes a list of expressions to select.
     * @return a query.
     */
    public static SelectQueryBuilder select(Node... nodes) {
        return Query.select(nodes);
    }

    /**
     * Creates a query that represents a WITH statement.
     *
     * @param ctes a list of CTE queries.
     * @return a WITH query.
     */
    public static WithQuery with(CteDef... ctes) {
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
    public static CteDef cte(String name) {
        return Query.cte(Identifier.of(name));
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
        return Query.cte(Identifier.of(name), body);
    }

    /**
     * Creates a CTE definition with the provided name and materialization hint.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     WITH cte AS MATERIALIZED (SELECT ...)
     *     }
     * </pre>
     *
     * @param name            the CTE name.
     * @param body            a sub query wrapped by the CTE.
     * @param columnAliases   a list of column aliases.
     * @param materialization materialization hint.
     * @return a CTE definition.
     */
    public static CteDef cte(String name, Query body, List<String> columnAliases, CteDef.Materialization materialization) {
        return Query.cte(
            Identifier.of(name),
            body,
            columnAliases == null ? null : columnAliases.stream().map(Identifier::of).toList(),
            materialization
        );
    }

    /**
     * Creates a CTE definition with the provided name and materialization hint.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     WITH cte AS MATERIALIZED (SELECT ...)
     *     }
     * </pre>
     *
     * @param name          the CTE name.
     * @param body          a sub query wrapped by the CTE.
     * @param columnAliases a list of column aliases.
     * @return a CTE definition.
     */
    public static CteDef cte(String name, Query body, List<String> columnAliases) {
        return Query.cte(
            Identifier.of(name),
            body,
            columnAliases == null ? null : columnAliases.stream().map(Identifier::of).toList()
        );
    }

    /**
     * Creates plain {@code DISTINCT} specification.
     *
     * @return a {@link DistinctSpec} representing {@code DISTINCT}
     */
    public static DistinctSpec distinct() {
        return DistinctSpec.TRUE;
    }

    /**
     * Creates {@code DISTINCT ON (...)} specification.
     *
     * @param items distinct expressions
     * @return a {@link DistinctSpec} representing {@code DISTINCT ON}
     */
    public static DistinctSpec distinctOn(Expression... items) {
        return DistinctSpec.on(List.of(items));
    }

    /**
     * Creates a LIMIT/OFFSET specification.
     *
     * @param limit  limit expression, may be null
     * @param offset offset expression, may be null
     * @return a limit/offset spec
     */
    public static LimitOffset limitOffset(Expression limit, Expression offset) {
        return LimitOffset.of(limit, offset);
    }

    /**
     * Creates {@code LIMIT ALL} specification.
     *
     * @return a limit/offset spec with {@code LIMIT ALL}
     */
    public static LimitOffset limitAll() {
        return LimitOffset.all();
    }

    /**
     * Creates {@code LIMIT ALL OFFSET <expr>} specification.
     *
     * @param offset offset expression
     * @return a limit/offset spec with {@code LIMIT ALL} and offset
     */
    public static LimitOffset limitAll(Expression offset) {
        return LimitOffset.of(null, offset, true);
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

    /* ========================= Locking Clause ========================= */

    /**
     * Returns the UPDATE lock mode for SELECT locking clauses.
     *
     * <p>Corresponds to {@code FOR UPDATE}.</p>
     *
     * @return UPDATE lock mode
     */
    public static LockMode update() {
        return LockMode.UPDATE;
    }

    /**
     * Returns the NO KEY UPDATE lock mode for SELECT locking clauses.
     *
     * <p>Corresponds to {@code FOR NO KEY UPDATE}.</p>
     *
     * @return NO KEY UPDATE lock mode
     */
    public static LockMode noKeyUpdate() {
        return LockMode.NO_KEY_UPDATE;
    }

    /**
     * Returns the SHARE lock mode for SELECT locking clauses.
     *
     * <p>Corresponds to {@code FOR SHARE}.</p>
     *
     * @return SHARE lock mode
     */
    public static LockMode share() {
        return LockMode.SHARE;
    }

    /**
     * Returns the KEY SHARE lock mode for SELECT locking clauses.
     *
     * <p>Corresponds to {@code FOR KEY SHARE}.</p>
     *
     * @return KEY SHARE lock mode
     */
    public static LockMode keyShare() {
        return LockMode.KEY_SHARE;
    }

    /**
     * Creates lock targets for a FOR ... OF locking clause.
     *
     * <p>Each identifier must refer to a table name or table alias visible
     * in the FROM clause.</p>
     *
     * <p>Example:</p>
     * <pre>
     * lockFor(update(), ofTables("t1", "orders"), false, false)
     * </pre>
     *
     * @param identifiers table names or aliases
     * @return list of lock targets
     * @throws IllegalArgumentException if any identifier is null or blank
     */
    public static List<LockTarget> ofTables(String... identifiers) {
        List<LockTarget> targets = new ArrayList<>(identifiers.length);
        for (String id : identifiers) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException(
                    "Lock target identifier cannot be null or blank"
                );
            }
            targets.add(LockTarget.of(Identifier.of(id)));
        }
        return targets;
    }

    /**
     * Creates lock targets for a FOR ... OF locking clause with the provided {@link QuoteStyle}.
     *
     * <p>Each identifier must refer to a table name or table alias visible
     * in the FROM clause.</p>
     *
     * <p>Example:</p>
     * <pre>
     * lockFor(update(), ofTables("t1", "orders"), false, false)
     * </pre>
     *
     * @param quoteStyle  quote style to user for table identifiers.
     * @param identifiers table names or aliases
     * @return list of lock targets
     * @throws IllegalArgumentException if any identifier is null or blank
     */
    public static List<LockTarget> ofTables(QuoteStyle quoteStyle, String... identifiers) {
        List<LockTarget> targets = new ArrayList<>(identifiers.length);
        for (String id : identifiers) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException(
                    "Lock target identifier cannot be null or blank"
                );
            }
            targets.add(LockTarget.of(Identifier.of(id, quoteStyle)));
        }
        return targets;
    }
}
