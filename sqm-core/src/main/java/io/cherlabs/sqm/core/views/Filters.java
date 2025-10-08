package io.cherlabs.sqm.core.views;

import io.cherlabs.sqm.core.*;
import io.cherlabs.sqm.core.traits.*;

import java.util.List;
import java.util.Optional;

/**
 * A view that provides access to {@link Filter} properties.
 */
public final class Filters {
    private Filters() {
    }

    /**
     * Gets a column from the filter implemented by one of the derived types if presented.
     *
     * @param f a filter.
     * @return {@link Optional} with the column if presented or an {@link Optional#empty()}.
     */
    public static Optional<Column> column(Filter f) {
        if (f instanceof HasColumn h) return Optional.ofNullable(h.column());
        return Optional.empty();
    }

    /**
     * Gets a list of columns from the filter implemented by one of the derived types if presented.
     *
     * @param f a filter.
     * @return {@link Optional} with the list of columns if presented or an {@link Optional#empty()}.
     */
    public static Optional<List<Column>> columns(Filter f) {
        if (f instanceof HasColumns h) return Optional.ofNullable(h.columns());
        return Optional.empty();
    }

    /**
     * Gets a column filter op from the filter implemented by one of the derived types if presented.
     *
     * @param f a filter.
     * @return {@link Optional} with the column filter op if presented or an {@link Optional#empty()}.
     */
    public static Optional<ColumnFilter.Operator> columnOperator(Filter f) {
        if (f instanceof HasColumnOperator h) return Optional.ofNullable(h.op());
        return Optional.empty();
    }

    /**
     * Gets a tuple filter op from the filter implemented by one of the derived types if presented.
     *
     * @param f a filter.
     * @return {@link Optional} with the tuple filter op if presented or an {@link Optional#empty()}.
     */
    public static Optional<TupleFilter.Operator> tupleOperator(Filter f) {
        if (f instanceof HasTupleOperator h) return Optional.ofNullable(h.operator());
        return Optional.empty();
    }

    /**
     * Gets values from the filter implemented by one of the derived types if presented.
     *
     * @param f a filter.
     * @return {@link Optional} with the filter values if presented or an {@link Optional#empty()}.
     */
    public static Optional<Values> values(Filter f) {
        if (f instanceof HasValues h) return Optional.ofNullable(h.values());
        return Optional.empty();
    }

    /**
     * Gets a composite filter op from the filter implemented by one of the derived types if presented.
     *
     * @param f a filter.
     * @return {@link Optional} with the composite filter op if presented or an {@link Optional#empty()}.
     */
    public static Optional<CompositeFilter.Operator> compositeOperator(Filter f) {
        if (f instanceof HasCompositeOperator h) return Optional.ofNullable(h.op());
        return Optional.empty();
    }

    /**
     * Gets a list of filters from the filter implemented by one of the derived types if presented.
     *
     * @param f a filter.
     * @return {@link Optional} with the list of filters if presented or an {@link Optional#empty()}.
     */
    public static Optional<List<Filter>> filters(Filter f) {
        if (f instanceof HasFilters h) return Optional.ofNullable(h.filters());
        return Optional.empty();
    }

    /**
     * Gets a filter expr from the filter implemented by one of the derived types if presented.
     *
     * @param f a filter.
     * @return {@link Optional} with the filter expr if presented or an {@link Optional#empty()}.
     */
    public static Optional<String> expr(Filter f) {
        if (f instanceof HasExpr h) return Optional.ofNullable(h.expr());
        return Optional.empty();
    }
}
