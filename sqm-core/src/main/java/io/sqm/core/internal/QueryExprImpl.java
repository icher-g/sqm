package io.sqm.core.internal;

import io.sqm.core.Query;
import io.sqm.core.QueryExpr;

/**
 * Represents a sub query expression.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SELECT *
 *     FROM (
 *       SELECT * FROM t
 *     )
 *     }
 * </pre>
 *
 * @param subquery a sub query wrapped as an expression.
 */
public record QueryExprImpl(Query subquery) implements QueryExpr {
}
