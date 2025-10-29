package io.sqm.core.internal;

import io.sqm.core.Query;
import io.sqm.core.QueryTable;

/**
 * Implements a query table. A wrapper for a query to be used in FROM statement.
 *
 * @param query a query to wrap.
 * @param alias an alias of the table.
 */
public record QueryTableImpl(Query query, String alias) implements QueryTable {
}
