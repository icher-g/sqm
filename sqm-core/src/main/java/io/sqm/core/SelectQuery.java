package io.sqm.core;

import io.sqm.core.internal.SelectQueryImpl;
import io.sqm.core.walk.NodeVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * A SELECT-style query.
 */
public non-sealed interface SelectQuery extends Query {

    /**
     * Creates instance of the SELECT statement.
     *
     * @return new instance of SELECT query.
     */
    static SelectQuery of() {
        return new SelectQueryImpl();
    }

    /**
     * Gets a list of select items to be used in SELECT statement.
     *
     * @return a list of select items.
     */
    List<SelectItem> items();

    /**
     * Adds items to the SELECT statement.
     *
     * @param nodes an array of SELECT items.
     * @return this.
     */
    default SelectQuery select(Node... nodes) {
        var items = new ArrayList<SelectItem>();
        for (var expr : nodes) {
            switch (expr) {
                case Expression expression -> items.add(expression.toSelectItem());
                case SelectItem selectItem -> items.add(selectItem);
                case Query query -> items.add(Expression.subquery(query).toSelectItem());
                default -> throw new IllegalStateException("The provided node is not supported in the SELECT clause: " + expr);
            }
        }
        return select(items);
    }

    /**
     * Adds items to the SELECT statement.
     *
     * @param items a list of items.
     * @return this.
     */
    SelectQuery select(List<SelectItem> items);

    /**
     * Gets a table reference used in a FROM statement.
     *
     * @return a table.
     */
    TableRef from();

    /**
     * Sets a table reference used in a FROM statement. The table can be a sub query.
     *
     * @param table a table reference.
     * @return this.
     */
    SelectQuery from(TableRef table);

    /**
     * Gets a list of joins.
     *
     * @return a list of joins.
     */
    List<Join> joins();

    /**
     * Adds joins to the query.
     *
     * @param joins an array of joins.
     * @return this.
     */
    default SelectQuery join(Join... joins) {
        return join(List.of(joins));
    }

    /**
     * Adds joins to the query.
     *
     * @param joins a list of joins.
     * @return this.
     */
    SelectQuery join(List<Join> joins);

    /**
     * Gets a predicate to be used in a WHERE statement.
     *
     * @return a predicate.
     */
    Predicate where();

    /**
     * Sets a predicate to be used in a WHERE statement.
     *
     * @param predicate a predicate.
     * @return this.
     */
    SelectQuery where(Predicate predicate);

    /**
     * Gets a list of GroupBy items.
     *
     * @return a list of group by items.
     */
    GroupBy groupBy();

    /**
     * Adds group by items to the query.
     *
     * @param items an array of group by items.
     * @return this.
     */
    default SelectQuery groupBy(GroupItem... items) {
        return groupBy(List.of(items));
    }

    /**
     * Adds group by items to the query.
     *
     * @param items a list of group by items.
     * @return this.
     */
    SelectQuery groupBy(List<GroupItem> items);

    /**
     * Gets a predicate to be used in a HAVING statement.
     *
     * @return a predicate.
     */
    Predicate having();

    /**
     * Sets a predicate to be used in a HAVING statement.
     *
     * @param predicate a predicate.
     * @return this.
     */
    SelectQuery having(Predicate predicate);

    /**
     * Gets a list of WINDOW specifications if there is any or NULL otherwise.
     * <p>Example of WINDOW specification:</p>
     * <pre>
     *     {@code
     *      SELECT
     *          dept,
     *          emp_name,
     *          salary,
     *          RANK() OVER w1        AS dept_rank,
     *          AVG(salary) OVER w2   AS overall_avg
     *      FROM employees
     *      WINDOW
     *          w1 AS (PARTITION BY dept ORDER BY salary DESC),
     *          w2 AS (ORDER BY salary DESC);
     *     }
     * </pre>
     *
     * @return a list of WINDOW specifications or NULL>
     */
    List<WindowDef> windows();

    /**
     * Adds WINDOW specification(s) to a query.
     *
     * @param windows a WINDOW specification(s).
     * @return this.
     */
    default SelectQuery window(WindowDef... windows) {
        return window(List.of(windows));
    }

    /**
     * Adds WINDOW specifications to a query.
     *
     * @param windows a WINDOW specifications.
     * @return this.
     */
    SelectQuery window(List<WindowDef> windows);

    /**
     * Gets a list of order by items.
     *
     * @return a list of oder by items.
     */
    OrderBy orderBy();             // null if absent

    /**
     * Adds order by items to the query.
     *
     * @param items an array of order by items.
     * @return this.
     */
    default SelectQuery orderBy(OrderItem... items) {
        return orderBy(List.of(items));
    }

    /**
     * Adds order by items to the query.
     *
     * @param items a list of group by items.
     * @return this.
     */
    SelectQuery orderBy(List<OrderItem> items);

    /**
     * Returns the DISTINCT specification of this SELECT query.
     *
     * <p>If this method returns {@code null}, the query does not apply any DISTINCT
     * semantics and behaves as a regular {@code SELECT}.</p>
     *
     * <p>If a {@link DistinctSpec} is present, its concrete implementation determines
     * the exact behavior. ANSI renderers may treat any non-null {@link DistinctSpec}
     * as plain {@code DISTINCT}, while dialect-specific renderers may apply more
     * specialized semantics (for example, PostgreSQL {@code DISTINCT ON}).</p>
     *
     * @return DISTINCT specification, or {@code null} if not present
     */
    DistinctSpec distinct();

    /**
     * Sets SELECT query with the given DISTINCT specification applied.
     *
     * <p>Passing {@code null} clears any existing DISTINCT specification.</p>
     *
     * <p>The concrete behavior of the DISTINCT clause is defined by the provided
     * {@link DistinctSpec} implementation and interpreted by the target SQL dialect
     * during rendering or validation.</p>
     *
     * @param spec DISTINCT specification to apply, or {@code null} to remove DISTINCT
     * @return this.
     */
    SelectQuery distinct(DistinctSpec spec);


    /**
     * Sets SELECT query with the given DISTINCT ON specification provided by the list of expressions.
     *
     * @param items a list of expressions to be used in DISTINCT ON (e1, e2) clause.
     * @return this.
     */
    default SelectQuery distinct(Expression... items) {
        return distinct(List.of(items));
    }

    /**
     * Sets SELECT query with the given DISTINCT ON specification provided by the list of expressions.
     *
     * @param items a list of expressions to be used in DISTINCT ON (e1, e2) clause.
     * @return this.
     */
    SelectQuery distinct(List<Expression> items);

    /**
     * Gets a limit of the query if there is any or NULL otherwise.
     *
     * @return a value of the limit.
     */
    Expression limit();

    /**
     * Gets a limit/offset specification for this query.
     *
     * @return a limit/offset specification or {@code null} if absent.
     */
    LimitOffset limitOffset();

    /**
     * Sets a limit/offset specification for this query.
     *
     * @param limitOffset a limit/offset specification or {@code null} to clear it.
     * @return this.
     */
    SelectQuery limitOffset(LimitOffset limitOffset);

    /**
     * Sets the query limit.
     *
     * @param limit a limit.
     * @return this.
     */
    SelectQuery limit(long limit);

    /**
     * Sets the query limit expression.
     *
     * @param limit a limit expression.
     * @return this.
     */
    SelectQuery limit(Expression limit);

    /**
     * Gets an offset of the query if there is any or NULL otherwise.
     *
     * @return a value of the offset.
     */
    Expression offset();

    /**
     * Sets the query offset.
     *
     * @param offset an offset.
     * @return this.
     */
    SelectQuery offset(long offset);

    /**
     * Sets the query offset expression.
     *
     * @param offset an offset expression.
     * @return this.
     */
    SelectQuery offset(Expression offset);

    /**
     * Returns the locking clause associated with this SELECT query.
     *
     * <p>If present, the locking clause controls row-level locking behavior
     * during query execution.</p>
     *
     * @return locking clause
     */
    LockingClause lockFor();

    /**
     * Returns SELECT query with the given locking clause.
     *
     * <p>The locking clause replaces any existing locking clause.</p>
     *
     * @param lockingClause locking clause to apply
     * @return this.
     */
    SelectQuery lockFor(LockingClause lockingClause);

    /**
     * Sets a locking clause based on the provided parameters.
     *
     * <p>This is a convenience method equivalent to creating a
     * {@link LockingClause} via {@link LockingClause#of(LockMode, List, boolean, boolean)}
     * and applying it to the query.</p>
     *
     * <p>Example:</p>
     * <pre>
     * select()
     *     .lockFor(update(), ofTables("t1", "t2"), false, true);
     * </pre>
     *
     * @param mode       lock mode
     * @param ofTables   tables affected by the lock, empty list means all tables
     * @param nowait     whether NOWAIT is specified
     * @param skipLocked whether SKIP LOCKED is specified
     * @return this.
     */
    SelectQuery lockFor(LockMode mode, List<LockTarget> ofTables, boolean nowait, boolean skipLocked);

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
        return v.visitSelectQuery(this);
    }
}
