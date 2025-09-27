package io.cherlabs.sqlmodel.core;

import java.util.List;

/**
 * Represents a composite query.
 * <p>Example for With statement:</p>
 * <pre>
 *     {@code
 *     WITH
 *     TABLE1 AS (
 *         SELECT * FROM SCHEMA.TABLE1
 *     ),
 *     TABLE2 AS (
 *         SELECT * FROM SCHEMA.TABLE2
 *     )
 *     SELECT * FROM TABLE T
 *     JOIN TABLE1 T1 ON ...
 *     JOIN TABLE2 T2 ON
 *     }
 * </pre>
 * <p>Example for Union statement:</p>
 * <pre>
 *     {@code
 *     (SELECT * FROM TABLE1)
 *     UNION
 *     (SELECT * FROM TABLE2)
 *     }
 * </pre>
 */
public class CompositeQuery extends Query {
    private final List<Query> queries;
    private final CompositionType compositionType;

    public CompositeQuery(List<Query> queries, CompositionType compositionType) {
        this.queries = queries;
        this.compositionType = compositionType;
    }

    public List<Query> getQueries() {
        return queries;
    }

    public CompositionType getCompositionType() {
        return compositionType;
    }
}
