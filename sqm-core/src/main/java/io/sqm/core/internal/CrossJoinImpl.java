package io.sqm.core.internal;

import io.sqm.core.CrossJoin;
import io.sqm.core.TableRef;

/**
 * Implements a CROSS JOIN. This produces the Cartesian product of both tables.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     SELECT *
 *     FROM employees
 *     CROSS JOIN departments;
 *     }
 * </pre>
 *
 * @param right the table to join.
 */
public record CrossJoinImpl(TableRef right) implements CrossJoin {
}
