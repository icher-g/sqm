package io.sqm.core;

import io.sqm.core.internal.SelectQueryImpl;
import io.sqm.core.walk.NodeVisitor;

import java.util.Arrays;
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
    List<SelectItem> select();

    /**
     * Adds items to the SELECT statement.
     *
     * @param items an array of items.
     * @return this.
     */
    default SelectQuery select(SelectItem... items) {
        return select(List.of(items));
    }

    /**
     * Adds expressions to the SELECT statement.
     *
     * @param expressions an array of expressions.
     * @return this.
     */
    default SelectQuery select(Expression... expressions) {
        return select(Arrays.stream(expressions)
                            .map(e -> (SelectItem) SelectItem.expr(e))
                            .toList());
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
     * Indicates if a DISTINCT keyword should be added to a SELECT statement.
     *
     * @return TRUE if a distinct needs to be added or FALSE otherwise.
     */
    Boolean distinct();

    /**
     * Sets distinct used in SELECT statement.
     *
     * @param distinct a value.
     * @return this.
     */
    SelectQuery distinct(boolean distinct);

    /**
     * Gets a limit of the query if there is any or NULL otherwise.
     *
     * @return a value of the limit.
     */
    Long limit();

    /**
     * Sets the query limit.
     *
     * @param limit a limit.
     * @return this.
     */
    SelectQuery limit(long limit);

    /**
     * Gets an offset of the query if there is any or NULL otherwise.
     *
     * @return a value of the offset.
     */
    Long offset();

    /**
     * Sets the query offset.
     *
     * @param offset an offset.
     * @return this.
     */
    SelectQuery offset(long offset);

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
