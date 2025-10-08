package io.cherlabs.sqm.core;

import io.cherlabs.sqm.core.traits.HasAlias;
import io.cherlabs.sqm.core.traits.HasQuery;

/**
 * Represents a sub query to be used as a column in SELECT statement.
 *
 * @param query a query.
 * @param alias an alias of the column.
 */
public record QueryColumn(Query<?> query, String alias) implements Column, HasQuery, HasAlias {
    /**
     * Adds an alias to the column.
     *
     * @param alias an alias.
     * @return A new instance with the provided alias. A query field is preserved.
     */
    public QueryColumn as(String alias) {
        return new QueryColumn(query, alias);
    }
}
