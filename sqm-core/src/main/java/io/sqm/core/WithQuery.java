package io.sqm.core;

import io.sqm.core.internal.WithQueryImpl;
import io.sqm.core.walk.NodeVisitor;

import java.util.List;

/**
 * <p>With statement example:</p>
 * <pre>
 *     {@code
 *     WITH
 *     TABLE1 AS (
 *         SELECT * FROM SCHEMA.TABLE1
 *     ),
 *     TABLE2 AS (
 *         SELECT * FROM SCHEMA.TABLE2
 *     )
 *     SELECT *
 *     FROM TABLE T
 *     JOIN TABLE1 T1 ON ...
 *     JOIN TABLE2 T2 ON
 *     }
 * </pre>
 */
public non-sealed interface WithQuery extends Query {

    /**
     * Creates a WITH query statement with the list of CTE sub queries and a body.
     *
     * @param ctes a list of CTE sub queries.
     * @param body a body.
     * @return A newly created WITH query.
     */
    static WithQuery of(List<CteDef> ctes, Query body) {
        return new WithQueryImpl(ctes, body, false);
    }

    /**
     * Creates a WITH query statement with the list of CTE sub queries and a body.
     *
     * @param ctes      a list of CTE sub queries.
     * @param body      a body.
     * @param recursive indicates whether the WITH statement supports recursive calls within the CTE queries.
     * @return A newly created WITH query.
     */
    static WithQuery of(List<CteDef> ctes, Query body, boolean recursive) {
        return new WithQueryImpl(ctes, body, recursive);
    }

    /**
     * Gets a list of CTE queries.
     *
     * @return a list of CTE queries.
     */
    List<CteDef> ctes();

    /**
     * Gets a body used at the end of the WITH statement.
     *
     * @return a WITH body query.
     */
    Query body();

    /**
     * Indicates whether the WITH statement supports recursive calls within the CTE queries.
     *
     * @return True if the recursive calls are supported and False otherwise.
     */
    boolean recursive();

    /**
     * Adds a recursive indication to the WITH query.
     *
     * @param recursive indicates whether the WITH statement is recursive or not.
     * @return A new instance of {@link WithQuery} with the select statement. All other fields are preserved.
     */
    default WithQuery recursive(boolean recursive) {
        return new WithQueryImpl(ctes(), body(), recursive);
    }

    /**
     * Adds SELECT statement to WITH query.
     *
     * @param body a SELECT statement.
     * @return this.
     */
    default WithQuery body(Query body) {
        return new WithQueryImpl(ctes(), body, recursive());
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
        return v.visitWithQuery(this);
    }
}
