package io.sqm.core.internal;

import io.sqm.core.CaseExpr;
import io.sqm.core.Expression;
import io.sqm.core.WhenThen;

import java.util.List;

/**
 * Represents a CASE expression.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     CASE WHEN x = 1 THEN 10 WHEN x = 2 THEN 20 END AS result
 *     }
 * </pre>
 *
 * @param whens    a list of WHEN...THEN statements.
 * @param elseExpr an ELSE expression.
 */
public record CaseExprImpl(List<WhenThen> whens, Expression elseExpr) implements CaseExpr {
}
