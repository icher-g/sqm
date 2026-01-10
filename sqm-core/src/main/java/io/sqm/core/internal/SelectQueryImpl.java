package io.sqm.core.internal;

import io.sqm.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SelectQueryImpl implements SelectQuery {

    private final List<SelectItem> items;
    private final List<Join> joins;
    private final List<WindowDef> windows;
    private GroupBy groupBy;
    private OrderBy orderBy;
    private TableRef tableRef;
    private Predicate where;
    private Predicate having;
    private DistinctSpec distinctSpec;
    private LimitOffset limitOffset;

    public SelectQueryImpl() {
        this.items = new ArrayList<>();
        this.joins = new ArrayList<>();
        this.windows = new ArrayList<>();
    }

    /**
     * Gets a list of select items to be used in SELECT statement.
     *
     * @return a list of select items.
     */
    @Override
    public List<SelectItem> items() {
        return items;
    }

    /**
     * Adds items to the SELECT statement.
     *
     * @param items a list of items.
     * @return this.
     */
    @Override
    public SelectQuery select(List<SelectItem> items) {
        this.items.addAll(items);
        return this;
    }

    /**
     * Gets a table reference used in a FROM statement.
     *
     * @return a table.
     */
    @Override
    public TableRef from() {
        return tableRef;
    }

    /**
     * Sets a table reference used in a FROM statement. The table can be a sub query.
     *
     * @param table a table reference.
     * @return this.
     */
    @Override
    public SelectQuery from(TableRef table) {
        this.tableRef = table;
        return this;
    }

    /**
     * Gets a list of joins.
     *
     * @return a list of joins.
     */
    @Override
    public List<Join> joins() {
        return joins;
    }

    /**
     * Adds joins to the query.
     *
     * @param joins a list of joins.
     * @return this.
     */
    @Override
    public SelectQuery join(List<Join> joins) {
        this.joins.addAll(joins);
        return this;
    }

    /**
     * Gets a predicate to be used in a WHERE statement.
     *
     * @return a predicate.
     */
    @Override
    public Predicate where() {
        return where;
    }

    /**
     * Sets a predicate to be used in a WHERE statement.
     *
     * @param predicate a predicate.
     * @return this.
     */
    @Override
    public SelectQuery where(Predicate predicate) {
        this.where = predicate;
        return this;
    }

    /**
     * Gets a list of GroupBy items.
     *
     * @return a list of group by items.
     */
    @Override
    public GroupBy groupBy() {
        return groupBy;
    }

    /**
     * Adds group by items to the query.
     *
     * @param items a list of group by items.
     * @return this.
     */
    @Override
    public SelectQuery groupBy(List<GroupItem> items) {
        this.groupBy = new GroupByImpl(items);
        return this;
    }

    /**
     * Gets a predicate to be used in a HAVING statement.
     *
     * @return a predicate.
     */
    @Override
    public Predicate having() {
        return having;
    }

    /**
     * Sets a predicate to be used in a HAVING statement.
     *
     * @param predicate a predicate.
     * @return this.
     */
    @Override
    public SelectQuery having(Predicate predicate) {
        this.having = predicate;
        return this;
    }

    /**
     * Gets a list of order by items.
     *
     * @return a list of oder by items.
     */
    @Override
    public OrderBy orderBy() {
        return orderBy;
    }

    /**
     * Adds order by items to the query.
     *
     * @param items a list of group by items.
     * @return this.
     */
    @Override
    public SelectQuery orderBy(List<OrderItem> items) {
        this.orderBy = new OrderByImpl(items);
        return this;
    }

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
    @Override
    public DistinctSpec distinct() {
        return distinctSpec;
    }

    /**
     * Returns a copy of this SELECT query with the given DISTINCT specification applied.
     *
     * <p>Passing {@code null} clears any existing DISTINCT specification.</p>
     *
     * <p>The concrete behavior of the DISTINCT clause is defined by the provided
     * {@link DistinctSpec} implementation and interpreted by the target SQL dialect
     * during rendering or validation.</p>
     *
     * @param spec DISTINCT specification to apply, or {@code null} to remove DISTINCT
     * @return new {@link SelectQuery} instance with the updated DISTINCT specification
     */
    @Override
    public SelectQuery distinct(DistinctSpec spec) {
        this.distinctSpec = spec;
        return this;
    }

    /**
     * Gets a limit of the query if there is any or NULL otherwise.
     *
     * @return a value of the limit.
     */
    @Override
    public Long limit() {
        return limitOffset == null ? null : limitOffset.limit();
    }

    /**
     * Sets the query limit.
     *
     * @param limit a limit.
     * @return this.
     */
    @Override
    public SelectQuery limit(long limit) {
        this.limitOffset = LimitOffset.of(limit, offset());
        return this;
    }

    /**
     * Gets an offset of the query if there is any or NULL otherwise.
     *
     * @return a value of the offset.
     */
    @Override
    public Long offset() {
        return limitOffset == null ? null : limitOffset.offset();
    }

    /**
     * Sets the query offset.
     *
     * @param offset an offset.
     * @return this.
     */
    @Override
    public SelectQuery offset(long offset) {
        this.limitOffset = LimitOffset.of(limit(), offset);
        return this;
    }

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
    @Override
    public List<WindowDef> windows() {
        return windows;
    }

    /**
     * Adds WINDOW specifications to a query.
     *
     * @param windows a WINDOW specifications.
     * @return this.
     */
    @Override
    public SelectQuery window(List<WindowDef> windows) {
        this.windows.addAll(windows);
        return this;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof SelectQuery that)) return false;

        return items.equals(that.items()) &&
            joins.equals(that.joins()) &&
            windows.equals(that.windows()) &&
            Objects.equals(groupBy, that.groupBy()) &&
            Objects.equals(orderBy, that.orderBy()) &&
            Objects.equals(tableRef, that.from()) &&
            Objects.equals(where, that.where()) &&
            Objects.equals(having, that.having()) &&
            Objects.equals(distinctSpec, that.distinct()) &&
            Objects.equals(limit(), that.limit()) &&
            Objects.equals(offset(), that.offset());
    }

    @Override
    public int hashCode() {
        int result = items.hashCode();
        result = 31 * result + joins.hashCode();
        result = 31 * result + windows.hashCode();
        result = 31 * result + Objects.hashCode(groupBy);
        result = 31 * result + Objects.hashCode(orderBy);
        result = 31 * result + Objects.hashCode(tableRef);
        result = 31 * result + Objects.hashCode(where);
        result = 31 * result + Objects.hashCode(having);
        result = 31 * result + Objects.hashCode(distinctSpec);
        result = 31 * result + Objects.hashCode(limitOffset);
        return result;
    }
}
