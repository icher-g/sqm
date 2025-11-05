package io.sqm.core.internal;

import io.sqm.core.FrameSpec;
import io.sqm.core.OrderBy;
import io.sqm.core.OverSpec;
import io.sqm.core.PartitionBy;

/**
 * OVER ( ... ) — inline window specification.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     SUM(salary) OVER (PARTITION BY dept ORDER BY hire_date)
 *     }
 * </pre>
 *
 * @param baseWindow the base window name. {@code OVER (w ORDER BY ... ROWS ...)}
 * @param partitionBy a {@code PARTITION BY e1, e2, ...} statement.
 * @param orderBy an {@code ORDER BY x [ASC|DESC] [NULLS {FIRST|LAST}], ...} statement.
 * @param frame a frame definition. {@code ROWS/RANGE/GROUPS frame (bounds + exclusion)}
 * @param exclude an exclusion.
 */
public record OverSpecDef(
    String baseWindow,            // expr.g., OVER (w ORDER BY ... ROWS ...)
    PartitionBy partitionBy,      // PARTITION BY e1, e2, ...
    OrderBy orderBy,              // ORDER BY x [ASC|DESC] [NULLS {FIRST|LAST}], ...
    FrameSpec frame,              // ROWS/RANGE/GROUPS frame (bounds + exclusion)
    OverSpec.Exclude exclude      // EXCLUDE clause
) implements OverSpec.Def {
}
