package io.cherlabs.sqm.core;

import java.util.ArrayList;
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
public class CteQuery extends Query<CteQuery> {
    private final List<String> columnAliases;

    public CteQuery() {
        this.columnAliases = new ArrayList<>();
    }

    /**
     * Gets a list of column aliases to be added into a CTE name for recursive usage.
     * @return a list of strings.
     */
    public List<String> columnAliases() {
        return columnAliases;
    }

    /**
     * Sets an array of column aliases to be added into a CTE name for recursive usage.
     * @param columnAliases an array of column aliases
     * @return a reference to this.
     */
    public CteQuery columnAliases(String... columnAliases) {
        this.columnAliases.addAll(List.of(columnAliases));
        return this;
    }
}
