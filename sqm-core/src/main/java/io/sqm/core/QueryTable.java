package io.sqm.core;

import io.sqm.core.internal.QueryTableImpl;

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
}
