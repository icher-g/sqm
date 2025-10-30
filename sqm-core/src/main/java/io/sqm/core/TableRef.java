package io.sqm.core;

import java.util.Optional;

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
     * Casts current table reference to {@link Table} is possible.
     *
     * @return {@link Optional}<{@link Table}>.
     */
    default Optional<Table> asTable() {
        return this instanceof Table t ? Optional.of(t) : Optional.empty();
    }

    /**
     * Casts current table reference to {@link QueryTable} is possible.
     *
     * @return {@link Optional}<{@link QueryTable}>.
     */
    default Optional<QueryTable> asQuery() {
        return this instanceof QueryTable t ? Optional.of(t) : Optional.empty();
    }

    /**
     * Casts current table reference to {@link ValuesTable} is possible.
     *
     * @return {@link Optional}<{@link ValuesTable}>.
     */
    default Optional<ValuesTable> asValues() {
        return this instanceof ValuesTable t ? Optional.of(t) : Optional.empty();
    }
}
