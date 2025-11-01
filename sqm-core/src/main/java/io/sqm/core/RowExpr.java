package io.sqm.core;

import io.sqm.core.internal.RowExprImpl;
import io.sqm.core.walk.NodeVisitor;

import java.util.List;

/**
 * Represents a row of values.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     WHERE a IN (1, 2, 3, 4)
 *     }
 * </pre>
 */
public non-sealed interface RowExpr extends ValueSet {
    /**
     * Gets a list of row values.
     *
     * @return a list of row values.
     */
    List<Expression> items();

    /**
     * Creates a new instance of the ListExpr from the row values.
     *
     * @param items a list of values.
     * @return A newly created instance of the ListExpr.
     */
    static RowExpr of(List<Expression> items) {
        return new RowExprImpl(items);
    }

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
        return v.visitRowExpr(this);
    }
}
