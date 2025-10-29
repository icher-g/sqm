package io.sqm.core.internal;

import io.sqm.core.ColumnExpr;

/**
 * Implements a column reference used in WHERE / JOIN / GROUP BY / ORDER BY etc.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     ON u.id = o.user_id;
 *     WHERE u.age > 18;
 *     GROUP BY u.country;
 *     HAVING COUNT(u.id) > 10;
 *     ORDER BY u.name;
 *     }
 * </pre>
 *
 * @param tableAlias a table name/alias. Can be NULL if the table is not used.
 * @param name       the name of the column.
 */
public record ColumnExprImpl(String tableAlias, String name) implements ColumnExpr {
}
