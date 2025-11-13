package io.sqm.core.internal;

import io.sqm.core.NaturalJoin;
import io.sqm.core.TableRef;

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
 *
 * @param right the right side of the join.
 */
public record NaturalJoinImpl(TableRef right) implements NaturalJoin {
}
