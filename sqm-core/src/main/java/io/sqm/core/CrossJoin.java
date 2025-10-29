package io.sqm.core;

/**
 * Represents a CROSS JOIN. This produces the Cartesian product of both tables.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     SELECT *
 *     FROM employees
 *     CROSS JOIN departments;
 *     }
 * </pre>
 */
public non-sealed interface CrossJoin extends Join {
}
