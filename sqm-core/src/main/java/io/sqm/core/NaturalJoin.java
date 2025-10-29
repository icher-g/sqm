package io.sqm.core;

/**
 * Implements a natural join.
 * A NATURAL JOIN is a type of join that automatically matches columns with the same name in both tables.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     SELECT *
 *     FROM employees
 *     NATURAL JOIN departments;
 *     }
 * </pre>
 */
public non-sealed interface NaturalJoin extends Join {
}
