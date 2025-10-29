package io.sqm.core.internal;

import io.sqm.core.RowExpr;
import io.sqm.core.RowListExpr;

import java.util.List;

/**
 * Represents a list of rows — the RHS list of row-value / row-tuples.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     a IN (1, 2, 3, 4)
 *     (a,b) IN ((1,2), (3,4))
 *     }
 * </pre>
 *
 * @param rows a list of rows.
 */
public record RowListExprImpl(List<RowExpr> rows) implements RowListExpr {
}
