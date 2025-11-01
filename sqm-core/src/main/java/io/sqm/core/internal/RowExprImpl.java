package io.sqm.core.internal;

import io.sqm.core.Expression;
import io.sqm.core.RowExpr;

import java.util.List;

/**
 * Represents a row of values.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     (1,2) used in (a,b) IN ((1,2),(3,4))
 *     }
 * </pre>
 *
 * @param items a list of row values.
 */
public record RowExprImpl(List<Expression> items) implements RowExpr {

    public RowExprImpl {
        items = List.copyOf(items);
    }
}
