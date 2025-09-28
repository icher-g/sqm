package io.cherlabs.sqlmodel.core;

import java.util.List;

/**
 * <p>With statement example:</p>
 * <pre>
 *     {@code
 *     WITH
 *     TABLE1 AS (
 *         SELECT * FROM SCHEMA.TABLE1
 *     ),
 *     TABLE2 AS (
 *         SELECT * FROM SCHEMA.TABLE2
 *     )
 *     SELECT *
 *     FROM TABLE T
 *     JOIN TABLE1 T1 ON ...
 *     JOIN TABLE2 T2 ON
 *     }
 * </pre>
 */
public class WithQuery extends Query<WithQuery> {
    private final List<? extends Query<?>> queries;
    private final boolean isRecursive;

    public WithQuery(List<Query<?>> queries, boolean isRecursive) {
        this.queries = queries;
        this.isRecursive = isRecursive;
    }

    public List<? extends Query<?>> getQueries() {
        return queries;
    }

    public boolean isRecursive() {
        return isRecursive;
    }
}
