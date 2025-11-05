package io.sqm.core.internal;

import io.sqm.core.BoundSpec;

/**
 * Represents an UNBOUNDED PRECEDING bound.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     -- All rows up to current
 *     SUM(salary) OVER (PARTITION BY dept ORDER BY salary DESC RANGE UNBOUNDED PRECEDING)
 *     }
 * </pre>
 */
public record BoundSpecUnboundedPreceding() implements BoundSpec.UnboundedPreceding {
}
