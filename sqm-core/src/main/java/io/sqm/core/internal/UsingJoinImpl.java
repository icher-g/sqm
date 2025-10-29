package io.sqm.core.internal;

import io.sqm.core.TableRef;
import io.sqm.core.UsingJoin;

import java.util.List;

/**
 * Implements a USING JOIN.
 * A USING join is a shorthand syntax for joining tables when the join column(s) have the same name in both tables.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     SELECT *
 *     FROM employees
 *     JOIN departments USING (department_id);
 *     }
 * </pre>
 *
 * @param right        the table to join.
 * @param usingColumns a list of columns to join on.
 */
public record UsingJoinImpl(TableRef right, List<String> usingColumns) implements UsingJoin {
}
