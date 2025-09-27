package io.cherlabs.sqlmodel.core;

import java.util.List;

public interface Filter extends Entity {
    static ColumnFilter column(Column column) {
        return new ColumnFilter(column, null, null);
    }

    static TupleFilter tuple(List<Column> columns) {
        return new TupleFilter(columns, null, null);
    }

    static CompositeFilter and(Filter... filters) {
        return new CompositeFilter(CompositeFilter.Operator.And, List.of(filters));
    }

    static CompositeFilter and(List<Filter> filters) {
        return new CompositeFilter(CompositeFilter.Operator.And, filters);
    }

    static CompositeFilter or(Filter... filters) {
        return new CompositeFilter(CompositeFilter.Operator.Or, List.of(filters));
    }

    static CompositeFilter or(List<Filter> filters) {
        return new CompositeFilter(CompositeFilter.Operator.Or, filters);
    }

    static CompositeFilter not(Filter filter) {
        return new CompositeFilter(CompositeFilter.Operator.Not, List.of(filter));
    }

    static ExpressionFilter expr(String exp) {
        return new ExpressionFilter(exp);
    }
}
