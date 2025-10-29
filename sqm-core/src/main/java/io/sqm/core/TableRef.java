package io.sqm.core;

/**
 * Anything that can appear in FROM/JOIN: table, subquery, VALUES, etc.
 */
public sealed interface TableRef extends Node permits QueryTable, Table, ValuesTable {
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
     * Creates a table with the provided name. All other fields are set to NULL.
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
    static ValuesTable values(RowListExpr rows) {
        return ValuesTable.of(rows);
    }

    /**
     * Gets table alias or null if none.
     *
     * @return a table alias.
     */
    String alias();

    /**
     * Casts current table reference to {@link Table}.
     *
     * @return {@link Table}.
     */
    default Table asTable() {
        return (Table) this;
    }

    /**
     * Casts current table reference to {@link QueryTable}.
     *
     * @return {@link QueryTable}.
     */
    default QueryTable asQuery() {
        return (QueryTable) this;
    }

    /**
     * Casts current table reference to {@link ValuesTable}.
     *
     * @return {@link ValuesTable}.
     */
    default ValuesTable asValues() {
        return (ValuesTable) this;
    }
}
