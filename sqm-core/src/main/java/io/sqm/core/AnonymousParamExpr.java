package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * An anonymous positional parameter, typically represented as {@code ?} in SQL.
 * <p>
 * Anonymous parameters do not carry an explicit index in the SQL text.
 * Instead, a logical position is assigned by the parser based on the
 * order in which parameters appear in the query.
 */
public non-sealed interface AnonymousParamExpr extends ParamExpr {

    /**
     * Creates a new anonymous positional parameter with the given position.
     *
     * @return an anonymous positional parameter
     */
    static AnonymousParamExpr of() {
        return new Impl();
    }

    /**
     * Accepts a visitor that processes this parameter node.
     *
     * @param v   the visitor
     * @param <R> the visitor return type
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitAnonymousParamExpr(this);
    }

    /**
     * An anonymous positional parameter, typically represented as {@code ?} in SQL.
     * <p>
     * Anonymous parameters do not carry an explicit index in the SQL text.
     * Instead, a logical position is assigned by the parser based on the
     * order in which parameters appear in the query.
     */
    record Impl() implements AnonymousParamExpr {
    }
}

