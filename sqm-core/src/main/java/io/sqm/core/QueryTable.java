package io.sqm.core;

import io.sqm.core.internal.QueryTableImpl;
import io.sqm.core.walk.NodeVisitor;

/**
 * A subquery used as a table source: (SELECT ...) AS alias
 */
public non-sealed interface QueryTable extends TableRef {

    /**
     * Wraps a query as a table for use in FROM statement.
     *
     * @param query a query to wrap.
     * @return A newly created instance of a wrapped query.
     */
    static QueryTable of(Query query) {
        return new QueryTableImpl(query, null);
    }

    /**
     * Gets a sub query.
     *
     * @return a sub query.
     */
    Query query();

    /**
     * Adds an alias to a query table.
     *
     * @param alias an alias to add.
     * @return this.
     */
    default QueryTable as(String alias) {
        return new QueryTableImpl(query(), alias);
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
        return v.visitQueryTable(this);
    }
}
