package io.cherlabs.sqm.core;

import java.util.List;

/**
 * A base interface for all filter implementations.
 */
public interface Filter extends Entity {
    /**
     * Creates a column filter with the provided column.
     *
     * @param column a column
     * @return A newly created instance of a filer.
     */
    static ColumnFilter column(Column column) {
        return new ColumnFilter(column, null, null);
    }

    /**
     * Creates a tuple filter with the provided columns.
     *
     * @param columns a list of columns.
     * @return A newly created instance of a filter.
     */
    static TupleFilter tuple(List<Column> columns) {
        return new TupleFilter(columns, null, null);
    }

    /**
     * Creates a composite filter with an {@link io.cherlabs.sqm.core.CompositeFilter.Operator#And} op.
     *
     * @param filters an array of filters to compose.
     * @return A newly created instance of a filter.
     */
    static CompositeFilter and(Filter... filters) {
        return new CompositeFilter(CompositeFilter.Operator.And, List.of(filters));
    }

    /**
     * Creates a composite filter with an {@link io.cherlabs.sqm.core.CompositeFilter.Operator#And} op.
     *
     * @param filters a list of filters to compose.
     * @return A newly created instance of a filter.
     */
    static CompositeFilter and(List<Filter> filters) {
        return new CompositeFilter(CompositeFilter.Operator.And, filters);
    }

    /**
     * Creates a composite filter with an {@link io.cherlabs.sqm.core.CompositeFilter.Operator#Or} op.
     *
     * @param filters an array of filters to compose.
     * @return A newly created instance of a filter.
     */
    static CompositeFilter or(Filter... filters) {
        return new CompositeFilter(CompositeFilter.Operator.Or, List.of(filters));
    }

    /**
     * Creates a composite filter with an {@link io.cherlabs.sqm.core.CompositeFilter.Operator#Or} op.
     *
     * @param filters a list of filters to compose.
     * @return A newly created instance of a filter.
     */
    static CompositeFilter or(List<Filter> filters) {
        return new CompositeFilter(CompositeFilter.Operator.Or, filters);
    }

    /**
     * Creates a composite filter with an {@link io.cherlabs.sqm.core.CompositeFilter.Operator#Not} op.
     *
     * @param filter a filter to negate.
     * @return A newly created instance of a filter.
     */
    static CompositeFilter not(Filter filter) {
        return new CompositeFilter(CompositeFilter.Operator.Not, List.of(filter));
    }

    /**
     * Creates a filter as a string expr.
     *
     * @param exp an expr.
     * @return A newly created instance of a filter.
     */
    static ExpressionFilter expr(String exp) {
        return new ExpressionFilter(exp);
    }
}
