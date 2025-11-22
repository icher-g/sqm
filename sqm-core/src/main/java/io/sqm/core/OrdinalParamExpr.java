package io.sqm.core;

import io.sqm.core.internal.OrdinalParamExprImpl;
import io.sqm.core.walk.NodeVisitor;

/**
 * A parameter that carries an explicit ordinal index.
 * <p>
 * Examples from different SQL environments include:
 * <ul>
 *   <li>{@code $1}, {@code $2}, ...}</li>
 *   <li>{@code ?1}, {@code ?2}, ...}</li>
 * </ul>
 * Regardless of how the parameter appeared in the original SQL text,
 * SQM only preserves the semantic information – the index itself.
 * The renderer for each dialect determines how the index is printed.
 */
public non-sealed interface OrdinalParamExpr extends ParamExpr {

    /**
     * Creates a new ordinal parameter with the given index.
     *
     * @param index 1-based index of the parameter
     * @return a new ordinal parameter expression
     */
    static OrdinalParamExpr of(int index) {
        return new OrdinalParamExprImpl(index);
    }

    /**
     * Returns the explicit 1-based index of this parameter.
     *
     * @return the ordinal index
     */
    int index();

    /**
     * Accepts a visitor that processes this parameter node.
     *
     * @param v   the visitor
     * @param <R> the visitor return type
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitOrdinalParamExpr(this);
    }
}

