package io.sqm.core.internal;

import io.sqm.core.CteDef;
import io.sqm.core.Query;

import java.util.List;

/**
 * A CTE definition used in a WITH statement.
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
 *
 * @param name the name of the CTE statement.
 * @param body a query wrapped by the current CTE.
 * @param columnAliases a list of column aliases used in the CTE.
 */
public record CteDefImpl(String name, Query body, List<String> columnAliases) implements CteDef {
}
