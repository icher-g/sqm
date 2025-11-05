package io.sqm.core.internal;

import io.sqm.core.BoundSpec;
import io.sqm.core.Expression;

/**
 * Represents a FOLLOWING bound.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     -- Next 3 rows
 *     AVG(salary) OVER (PARTITION BY dept ORDER BY salary ROWS 3 FOLLOWING)
 *     }
 * </pre>
 *
 * @param expr an expression.
 */
public record BoundSpecFollowing(Expression expr) implements BoundSpec.Following {
}
