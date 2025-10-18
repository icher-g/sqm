package io.sqm.core;

import io.sqm.core.traits.HasBody;
import io.sqm.core.traits.HasCtes;
import io.sqm.core.traits.HasRecursive;

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
public record WithQuery(Query body, List<CteQuery> ctes, boolean recursive) implements Query, HasBody, HasCtes, HasRecursive {

    /**
     * Adds a select statement to the WITH query.
     *
     * @param body a select statement.
     * @return A new instance of {@link WithQuery} with the select statement. All other fields are preserved.
     */
    public WithQuery select(Query body) {
        return new WithQuery(body, ctes, recursive);
    }

    /**
     * Adds a recursive indication to the WITH query.
     *
     * @param recursive indicates whether the WITH statement is recursive or not.
     * @return A new instance of {@link WithQuery} with the select statement. All other fields are preserved.
     */
    public WithQuery recursive(boolean recursive) {
        return new WithQuery(body, ctes, recursive);
    }
}
