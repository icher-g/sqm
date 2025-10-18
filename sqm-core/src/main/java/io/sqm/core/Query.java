package io.sqm.core;

import java.util.List;

/**
 * Represents a base interface for all query implementations.
 */
public interface Query extends Entity {

    /**
     * Creates a {@link SelectQuery} with the list of columns.
     *
     * @param columns a list of columns to select.
     * @return a query.
     */
    static SelectQuery select(Column... columns) {
        return new SelectQuery().select(columns);
    }

    /**
     * Creates a query that represents a WITH statement.
     *
     * @param ctes a list of CTE queries.
     * @return a WITH query.
     */
    static WithQuery with(CteQuery... ctes) {
        return new WithQuery(null, List.of(ctes), false);
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
    static CteQuery cte(String name) {
        return new CteQuery(name, null, List.of());
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
    static CompositeQuery composite(List<Query> terms, List<CompositeQuery.Op> ops) {
        return new CompositeQuery(terms, ops);
    }
}
