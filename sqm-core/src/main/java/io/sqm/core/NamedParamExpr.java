package io.sqm.core;

import io.sqm.core.internal.NamedParamExprImpl;
import io.sqm.core.walk.NodeVisitor;

/**
 * A parameter identified by a symbolic name rather than a numeric index.
 * <p>
 * Named parameters may appear in different syntactic forms depending on the
 * source SQL dialect (e.g., {@code :id}, {@code @id}, {@code #{id}}).
 * SQM stores only the canonical name without any prefix.
 */
public non-sealed interface NamedParamExpr extends ParamExpr {

    /**
     * Creates a new named parameter with the given canonical name.
     *
     * @param name parameter name without prefix
     * @return a named parameter expression
     */
    static NamedParamExpr of(String name) {
        return new NamedParamExprImpl(name);
    }

    /**
     * Returns the canonical name of the parameter (e.g. {@code "id"}).
     *
     * @return the parameter name
     */
    String name();

    /**
     * Accepts a visitor that processes this parameter node.
     *
     * @param v   the visitor
     * @param <R> the visitor return type
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitNamedParamExpr(this);
    }
}

