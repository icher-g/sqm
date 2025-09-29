package io.cherlabs.sqlmodel.core;

import io.cherlabs.sqlmodel.core.traits.HasAlias;
import io.cherlabs.sqlmodel.core.traits.HasQuery;

/**
 * Represents a sub query used in a FROM statement.
 *
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SELECT *
 *     FROM (
 *      SELECT * FROM t
 *     )
 *     }
 * </pre>
 *
 * @param query a query.
 * @param alias an alias for a query.
 */
public record QueryTable(Query<?> query, String alias) implements Table, HasQuery, HasAlias {
}
