package io.sqm.core.internal;

import io.sqm.core.Direction;
import io.sqm.core.Expression;
import io.sqm.core.Nulls;
import io.sqm.core.OrderItem;

/**
 * Represents an order by item.
 *
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     ORDER BY products.name
 *     ORDER BY 1, 2
 *     }
 * </pre>
 *
 * @param expr      an expression to be used in ORDER BY statement. Can be null if ordinal is used.
 * @param ordinal   an ordinal to be used in ORDER BY statement. 1-based; may be null if expression is used.
 * @param direction an ORDER BY direction: ASC, DESC.
 * @param nulls     the definition of how NULLs should be treated in the ORDER BY statement.
 * @param collate   optional collation name; may be null.
 */
public record OrderItemImpl(Expression expr, Integer ordinal, Direction direction, Nulls nulls, String collate) implements OrderItem {
}
