package io.cherlabs.sqm.core;

import io.cherlabs.sqm.core.traits.HasBody;
import io.cherlabs.sqm.core.traits.HasColumnAliases;
import io.cherlabs.sqm.core.traits.HasName;

import java.util.List;

/**
 * Represents a CTE query used in a WITH statement.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *      WITH RECURSIVE
 *        -- Non-recursive CTE (perfectly fine to mix)
 *        roots AS (
 *          SELECT id
 *          FROM employees
 *          WHERE manager_id IS NULL
 *        ),
 *
 *        -- Recursive CTE (self-reference to `chain`)
 *        chain AS (
 *          -- Anchor: start from roots
 *          SELECT e.id, e.manager_id, 1 AS lvl
 *          FROM employees e
 *          JOIN roots r ON e.id = r.id
 *
 *          UNION ALL
 *
 *          -- Recursive step: walk down the tree
 *          SELECT e.id, e.manager_id, c.lvl + 1
 *          FROM employees e
 *          JOIN chain c ON e.manager_id = c.id
 *        )
 *
 *      SELECT id, manager_id, lvl
 *      FROM chain
 *      ORDER BY lvl, id;
 *     }
 * </pre>
 */
public record CteQuery(String name, Query body, List<String> columnAliases) implements Query, HasName, HasBody, HasColumnAliases {

    /**
     * Adds a select statement to the WITH query.
     *
     * @param body a select statement.
     * @return A new instance of {@link CteQuery} with the select statement. All other fields are preserved.
     */
    public CteQuery select(Query body) {
        return new CteQuery(name, body, columnAliases);
    }

    /**
     * Adds a list of column aliases to the WITH query.
     *
     * @param columnAliases a list of column aliases.
     * @return A new instance of {@link CteQuery} with the list of column aliases. All other fields are preserved.
     */
    public CteQuery columnAliases(List<String> columnAliases) {
        return new CteQuery(name, body, columnAliases);
    }

    /**
     * Adds a list of column aliases to the WITH query.
     *
     * @param columnAliases a list of column aliases.
     * @return A new instance of {@link CteQuery} with the list of column aliases. All other fields are preserved.
     */
    public CteQuery columnAliases(String... columnAliases) {
        return new CteQuery(name, body, List.of(columnAliases));
    }
}
