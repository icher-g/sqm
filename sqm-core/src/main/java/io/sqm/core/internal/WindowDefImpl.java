package io.sqm.core.internal;

import io.sqm.core.OverSpec;
import io.sqm.core.WindowDef;

/**
 * Implements a WINDOW specification used in OVER clause.
 * <p>Example of WINDOW specification:</p>
 * <pre>
 *     {@code
 *      SELECT
 *          dept,
 *          emp_name,
 *          salary,
 *          RANK() OVER w1        AS dept_rank,
 *          AVG(salary) OVER w2   AS overall_avg
 *      FROM employees
 *      WINDOW
 *          w1 AS (PARTITION BY dept ORDER BY salary DESC),
 *          w2 AS (ORDER BY salary DESC);
 *     }
 * </pre>
 *
 * @param name a window name.
 * @param spec an OVER specification.
 */
public record WindowDefImpl(String name, OverSpec.Def spec) implements WindowDef {
}
