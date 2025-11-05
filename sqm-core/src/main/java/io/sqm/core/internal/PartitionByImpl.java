package io.sqm.core.internal;

import io.sqm.core.Expression;
import io.sqm.core.PartitionBy;

import java.util.List;

/**
 * Implements a PARTITION BY statement used in OVER();
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SUM(salary) OVER (PARTITION BY dept ORDER BY salary DESC RANGE UNBOUNDED PRECEDING)
 *     AVG(salary) OVER (PARTITION BY dept ORDER BY salary ROWS CURRENT ROW)
 *     }
 * </pre>
 *
 * @param items a list of expressions used in PARTITION BY statement.
 */
public record PartitionByImpl(List<Expression> items) implements PartitionBy {

    public PartitionByImpl {
        items = List.copyOf(items);
    }
}
