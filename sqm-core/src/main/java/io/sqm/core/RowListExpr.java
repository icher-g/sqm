package io.sqm.core;

import io.sqm.core.internal.RowListExprImpl;

import java.util.List;

/**
 * Represents a list of rows — the RHS list of row-value / row-tuples.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     (a,b) IN ((1,2), (3,4))
 *     }
 * </pre>
 */
public non-sealed interface RowListExpr extends ValueSet {
    /**
     * Creates a list of rows expressions. {@code a IN (1, 2, 3, 4) | (a,b) IN ((1,2), (3,4))}
     *
     * @param rows a list of expressions.
     * @return A newly created instance of the {@link RowListExpr}.
     */
    static RowListExpr of(List<RowExpr> rows) {
        return new RowListExprImpl(rows);
    }

    /**
     * Gets a list of rows.
     *
     * @return a list of rows.
     */
    List<RowExpr> rows();
}
