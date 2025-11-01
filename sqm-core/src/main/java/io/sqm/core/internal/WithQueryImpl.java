package io.sqm.core.internal;

import io.sqm.core.CteDef;
import io.sqm.core.Query;
import io.sqm.core.WithQuery;

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
 *
 * @param ctes      a list of CTE queries.
 * @param body      a body query used at the end of the WITH statement.
 * @param recursive indicates whether the WITH statement supports recursive calls within the CTE queries.
 */
public record WithQueryImpl(List<CteDef> ctes, Query body, boolean recursive) implements WithQuery {

    public WithQueryImpl {
        ctes = List.copyOf(ctes);
    }
}
