package io.sqm.core.internal;

import io.sqm.core.BoundSpec;
import io.sqm.core.Expression;

/**
 * Represents a PRECEDING bound.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     -- Last 5 rows
 *     SUM(salary) OVER (PARTITION BY dept ORDER BY salary DESC ROWS 5 PRECEDING)
 *     }
 * </pre>
 *
 * @param expr an expression.
 */
public record BoundSpecPreceding(Expression expr) implements BoundSpec.Preceding {
}
