package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

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
 *          SELECT expr.id, expr.manager_id, 1 AS lvl
 *          FROM employees expr
 *          JOIN roots r ON expr.id = r.id
 *
 *          UNION ALL
 *
 *          -- Recursive step: walk down the tree
 *          SELECT expr.id, expr.manager_id, c.lvl + 1
 *          FROM employees expr
 *          JOIN chain c ON expr.manager_id = c.id
 *        )
 *
 *      SELECT id, manager_id, lvl
 *      FROM chain
 *      ORDER BY lvl, id;
 *     }
 * </pre>
 */
public non-sealed interface CteDef extends Node {

    /**
     * Creates a CTE definition with the provided name.
     *
     * @param name the CTE name.
     * @return A newly created CTE definition.
     */
    static CteDef of(String name) {
        return new Impl(name, null, null);
    }

    /**
     * Creates a CTE definition with the provided name.
     *
     * @param name the CTE name.
     * @param body a sub query wrapped by the CTE.
     * @return A newly created CTE definition.
     */
    static CteDef of(String name, Query body) {
        return new Impl(name, body, null);
    }

    /**
     * Creates a CTE definition with the provided name.
     *
     * @param name the CTE name.
     * @param body a sub query wrapped by the CTE.
     * @param columnAliases a list of column aliases.
     * @return A newly created CTE definition.
     */
    static CteDef of(String name, Query body, List<String> columnAliases) {
        return new Impl(name, body, columnAliases);
    }

    /**
     * Gets the name of the CTE statement.
     *
     * @return a CTE name.
     */
    String name();

    /**
     * Gets a query wrapped by the current CTE.
     *
     * @return a query.
     */
    Query body();

    /**
     * Gets a list of column aliases used in the CTE.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     WITH RECURSIVE t(n) AS ( -- n is a column alias.
     *        SELECT 1
     *        UNION ALL
     *        SELECT n + 1 FROM t WHERE n < 10
     *     )
     *     SELECT * FROM t
     *     }
     * </pre>
     *
     * @return a list of column aliases.
     */
    List<String> columnAliases();

    /**
     * Adds SELECT statement to CTE statement.
     *
     * @param body a SELECT statement.
     * @return this.
     */
    default CteDef body(Query body) {
        return new Impl(name(), body, columnAliases());
    }

    /**
     * Adds a list of column aliases to the WITH query.
     *
     * @param columnAliases a list of column aliases.
     * @return A new instance of {@link CteDef} with the list of column aliases. All other fields are preserved.
     */
    default CteDef columnAliases(List<String> columnAliases) {
        return new Impl(name(), body(), columnAliases);
    }

    /**
     * Adds a list of column aliases to the WITH query.
     *
     * @param columnAliases a list of column aliases.
     * @return A new instance of {@link CteDef} with the list of column aliases. All other fields are preserved.
     */
    default CteDef columnAliases(String... columnAliases) {
        return new Impl(name(), body(), List.of(columnAliases));
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
        return v.visitCte(this);
    }

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
     *          SELECT expr.id, expr.manager_id, 1 AS lvl
     *          FROM employees expr
     *          JOIN roots r ON expr.id = r.id
     *
     *          UNION ALL
     *
     *          -- Recursive step: walk down the tree
     *          SELECT expr.id, expr.manager_id, c.lvl + 1
     *          FROM employees expr
     *          JOIN chain c ON expr.manager_id = c.id
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
    record Impl(String name, Query body, List<String> columnAliases) implements CteDef {
    }
}
