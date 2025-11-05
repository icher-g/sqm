package io.sqm.core.internal;

import io.sqm.core.BoundSpec;

/**
 * Represents a CURRENT ROW bound.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     -- Only current row
 *     AVG(salary) OVER (PARTITION BY dept ORDER BY salary ROWS CURRENT ROW)
 *     }
 * </pre>
 */
public record BoundSpecCurrentRow() implements BoundSpec.CurrentRow {
}
