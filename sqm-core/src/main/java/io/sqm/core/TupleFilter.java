package io.sqm.core;

import io.sqm.core.traits.HasColumns;
import io.sqm.core.traits.HasTupleOperator;
import io.sqm.core.traits.HasValues;

import java.util.List;

/**
 * Represents a filter for tuples.
 * For example: {@code WHERE (c1, c2) IN ((1, 2), (3, 4))}
 *
 * @param columns  a list of the columns to be used in a filter.
 * @param operator a filter op.
 * @param values   values for a filter.
 */
public record TupleFilter(List<Column> columns, Operator operator, Values values) implements Filter, HasColumns, HasTupleOperator, HasValues {
    /**
     * Creates an {@link Operator#In} filter with the provided tuple values.
     *
     * @param rows a list of tuples.
     * @return A new instance with the provided op. The columns filed is preserved.
     */
    public TupleFilter in(List<List<Object>> rows) {
        return new TupleFilter(columns, Operator.In, Values.tuples(rows));
    }

    /**
     * Creates an {@link Operator#NotIn} filter with the provided tuple values.
     *
     * @param rows a list of tuples.
     * @return A new instance with the provided op. The columns filed is preserved.
     */
    public TupleFilter notIn(List<List<Object>> rows) {
        return new TupleFilter(columns, Operator.NotIn, Values.tuples(rows));
    }

    /**
     * Returns values cast to a specific type.
     *
     * @param type the type of the values to cast to.
     * @param <T>  the type of the values to cast.
     * @return values
     */
    public <T extends Values> T valuesAs(Class<T> type) {
        return type.cast(values());
    }

    public enum Operator {
        In,
        NotIn
    }
}
