package io.sqm.core.internal;

import io.sqm.core.Expression;
import io.sqm.core.ExprSelectItem;

/**
 * Represents a wrapped expression in a SELECT statement.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SELECT products.name AS name -- ExprSelectItem(ColumnRef("products", "name"), "name")
 *     }
 * </pre>
 *
 * @param expr an expression to wrap.
 * @param alias an alias.
 */
public record ExprSelectItemImpl(Expression expr, String alias) implements ExprSelectItem {
}
