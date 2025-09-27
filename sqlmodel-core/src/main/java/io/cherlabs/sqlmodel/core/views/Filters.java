package io.cherlabs.sqlmodel.core.views;

import io.cherlabs.sqlmodel.core.*;
import io.cherlabs.sqlmodel.core.traits.*;

import java.util.List;
import java.util.Optional;

public final class Filters {
    private Filters() {
    }

    public static Optional<Column> column(Filter f) {
        if (f instanceof HasColumn h) return Optional.ofNullable(h.column());
        return Optional.empty();
    }

    public static Optional<ColumnFilter.Operator> columnOperator(Filter f) {
        if (f instanceof HasColumnOperator h) return Optional.ofNullable(h.operator());
        return Optional.empty();
    }

    public static Optional<TupleFilter.Operator> tupleOperator(Filter f) {
        if (f instanceof HasTupleOperator h) return Optional.ofNullable(h.operator());
        return Optional.empty();
    }

    public static Optional<Values> values(Filter f) {
        if (f instanceof HasValues h) return Optional.ofNullable(h.values());
        return Optional.empty();
    }

    public static Optional<CompositeFilter.Operator> compositeOperator(Filter f) {
        if (f instanceof HasCompositeOperator h) return Optional.ofNullable(h.operator());
        return Optional.empty();
    }

    public static Optional<List<Filter>> filters(Filter f) {
        if (f instanceof HasFilters h) return Optional.ofNullable(h.filters());
        return Optional.empty();
    }

    public static Optional<String> expr(Filter f) {
        if (f instanceof HasExpr h) return Optional.ofNullable(h.expression());
        return Optional.empty();
    }
}
