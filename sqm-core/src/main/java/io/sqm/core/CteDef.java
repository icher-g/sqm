package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.stream.Stream;

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
     * Materialization hint for CTEs.
     */
    enum Materialization {
        /**
         * No explicit hint.
         */
        DEFAULT,
        /**
         * Forces materialization (PostgreSQL).
         */
        MATERIALIZED,
        /**
         * Prevents materialization (PostgreSQL).
         */
        NOT_MATERIALIZED
    }

    /**
     * Creates a CTE definition with the provided name identifier.
     *
     * @param name the CTE name identifier.
     * @return a newly created CTE definition.
     */
    static CteDef of(Identifier name) {
        return of(name, null, null, Materialization.DEFAULT);
    }

    /**
     * Creates a CTE definition with the provided name identifier.
     *
     * @param name the CTE name identifier.
     * @param body a sub query wrapped by the CTE.
     * @return a newly created CTE definition.
     */
    static CteDef of(Identifier name, Query body) {
        return of(name, body, null, Materialization.DEFAULT);
    }

    /**
     * Creates a CTE definition with the provided name identifier.
     *
     * @param name the CTE name identifier.
     * @param body a sub query wrapped by the CTE.
     * @param columnAliases a list of column alias identifiers.
     * @return a newly created CTE definition.
     */
    static CteDef of(Identifier name, Query body, List<Identifier> columnAliases) {
        return of(name, body, columnAliases, Materialization.DEFAULT);
    }

    /**
     * Creates a CTE definition with the provided name identifier.
     *
     * @param name the CTE name identifier.
     * @param body a sub query wrapped by the CTE.
     * @param columnAliases a list of column alias identifiers.
     * @param materialization materialization hint.
     * @return a newly created CTE definition.
     */
    static CteDef of(Identifier name, Query body, List<Identifier> columnAliases, Materialization materialization) {
        return new Impl(name, body, columnAliases, materialization);
    }

    /**
     * Gets the name of the CTE statement.
     *
     * @return a CTE name.
     */
    Identifier name();

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
    List<Identifier> columnAliases();

    /**
     * Gets materialization hint for this CTE.
     *
     * @return materialization hint.
     */
    Materialization materialization();

    /**
     * Adds SELECT statement to CTE statement.
     *
     * @param body a SELECT statement.
     * @return this.
     */
    default CteDef body(Query body) {
        return of(name(), body, columnAliases(), materialization());
    }

    /**
     * Adds a list of column aliases to the WITH query.
     *
     * @param columnAliases a list of column aliases.
     * @return A new instance of {@link CteDef} with the list of column aliases. All other fields are preserved.
     */
    default CteDef columnAliases(List<Identifier> columnAliases) {
        return of(name(), body(), columnAliases, materialization());
    }

    /**
     * Adds a list of column aliases to the WITH query.
     *
     * @param columnAliases a list of column alias identifiers.
     * @return a new instance of {@link CteDef} with the list of column aliases. All other fields are preserved.
     */
    default CteDef columnAliases(Identifier... columnAliases) {
        return columnAliases(List.of(columnAliases));
    }

    /**
     * Adds a list of column aliases to the WITH query using string convenience values.
     * <p>
     * This is a helper for DSL/model authoring. Parsers and internal transformations should prefer
     * identifier-preserving APIs.
     *
     * @param columnAliases a list of column alias values.
     * @return a new instance of {@link CteDef} with the list of column aliases. All other fields are preserved.
     */
    default CteDef columnAliases(String... columnAliases) {
        return columnAliases(Stream.of(columnAliases).map(Identifier::of).toList());
    }

    /**
     * Adds materialization hint to the CTE.
     *
     * @param materialization materialization hint.
     * @return A new instance of {@link CteDef} with the provided hint. All other fields are preserved.
     */
    default CteDef materialization(Materialization materialization) {
        return of(name(), body(), columnAliases(), materialization);
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
     * @param materialization materialization hint.
     */
    record Impl(Identifier name, Query body, List<Identifier> columnAliases, Materialization materialization) implements CteDef {
        /**
         * Creates a CTE implementation.
         *
         * @param name                   CTE name identifier
         * @param body                   query body
         * @param columnAliases          CTE column alias identifiers
         * @param materialization        materialization hint
         */
        public Impl {
            columnAliases = columnAliases == null ? null : List.copyOf(columnAliases);
        }
    }
}
