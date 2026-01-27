package io.sqm.core;

import io.sqm.core.match.TableRefMatch;

/**
 * Anything that can appear in FROM/JOIN: table, subquery, VALUES, etc.
 */
public sealed interface TableRef extends FromItem permits AliasedTableRef, DialectTableRef, Lateral, Table {
    /**
     * Creates a table with the provided name. All other fields are set to NULL.
     *
     * @param name the name of the table. This is not qualified name.
     * @return A newly created instance of the table.
     */
    static Table table(String name) {
        return Table.of(name);
    }

    /**
     * Creates a table with the provided name.
     *
     * @param name   the name of the table. This is not qualified name.
     * @param schema a table schema.
     * @return A newly created instance of the table.
     */
    static Table table(String schema, String name) {
        return Table.of(schema, name);
    }

    /**
     * Wraps a query as a table.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     SELECT * FROM (SELECT * FROM t);
     *     }
     * </pre>
     *
     * @param query a query to wrap as a table.
     * @return {@link QueryTable}.
     */
    static QueryTable query(Query query) {
        return QueryTable.of(query);
    }

    /**
     * Creates a VALUES statement used as a table.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     (VALUES (1, 'A'), (2, 'B')) AS v(id, name)
     *     }
     * </pre>
     *
     * @param rows a set of values.
     * @return A newly created instance of the values table.
     */
    static ValuesTable values(RowValues rows) {
        return ValuesTable.of(rows);
    }

    /**
     * Creates a {@link FunctionTable} from the given function expression.
     * <p>
     * This factory method is a convenience for treating a {@link FunctionExpr}
     * as a table-producing expression in a {@code FROM} clause.
     * <p>
     * The resulting table reference has no alias or derived column list.
     * If correlation with preceding FROM items is required, it may be wrapped
     * as lateral using {@link TableRef#lateral()}.
     *
     * @param expr the function expression that produces table rows
     * @return a {@link FunctionTable} representing the function as a table reference
     */
    static FunctionTable function(FunctionExpr expr) {
        return FunctionTable.of(expr);
    }

    /**
     * Wraps this table reference as a lateral FROM item.
     * <p>
     * A lateral table reference is evaluated with access to columns of
     * preceding FROM items in the same FROM clause, enabling correlated
     * subqueries and table-valued expressions.
     * <p>
     * This is a convenience method equivalent to explicitly creating
     * a lateral wrapper around this table reference.
     *
     * @return a lateral wrapper for this table reference
     */
    default Lateral lateral() {
        return Lateral.of(this);
    }

    /**
     * Creates a new matcher for the current {@link TableRef}.
     *
     * @param <R> the result type
     * @return a new {@code TableMatch}.
     */
    default <R> TableRefMatch<R> matchTableRef() {
        return TableRefMatch.match(this);
    }
}
