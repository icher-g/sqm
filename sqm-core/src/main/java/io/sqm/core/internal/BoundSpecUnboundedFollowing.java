package io.sqm.core.internal;

import io.sqm.core.BoundSpec;

/**
 * Represents an UNBOUNDED FOLLOWING bound.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     -- All future rows
 *     MAX(value) OVER (ORDER BY ts RANGE UNBOUNDED FOLLOWING)
 *     }
 * </pre>
 */
public record BoundSpecUnboundedFollowing() implements BoundSpec.UnboundedFollowing {
}
