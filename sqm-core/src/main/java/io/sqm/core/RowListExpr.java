package io.sqm.core;

import io.sqm.core.internal.RowListExprImpl;
import io.sqm.core.walk.NodeVisitor;

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

    /**
     * Accepts a {@link NodeVisitor} and dispatches control to the
     * visitor method corresponding to the concrete subtype
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type returned by the visitor
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitRowListExpr(this);
    }
}
