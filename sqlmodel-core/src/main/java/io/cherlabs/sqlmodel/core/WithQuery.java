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
    private final List<? extends Query<?>> ctes;
    private boolean isRecursive;

    public WithQuery(List<? extends Query<?>> ctes) {
        this.ctes = ctes;
    }

    public List<? extends Query<?>> ctes() {
        return ctes;
    }

    public boolean isRecursive() {
        return isRecursive;
    }

    public WithQuery isRecursive(boolean isRecursive) {
        this.isRecursive = isRecursive;
        return this;
    }
}
