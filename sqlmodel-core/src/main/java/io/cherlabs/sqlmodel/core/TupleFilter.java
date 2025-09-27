package io.cherlabs.sqlmodel.core;

import io.cherlabs.sqlmodel.core.traits.HasColumns;
import io.cherlabs.sqlmodel.core.traits.HasTupleOperator;
import io.cherlabs.sqlmodel.core.traits.HasValues;

import java.util.List;

public record TupleFilter(List<Column> columns, Operator operator, Values values) implements Filter, HasColumns, HasTupleOperator, HasValues {
    public TupleFilter in(List<List<Object>> rows) {
        return new TupleFilter(columns, Operator.In, Values.tuples(rows));
    }

    public TupleFilter notIn(List<List<Object>> rows) {
        return new TupleFilter(columns, Operator.NotIn, Values.tuples(rows));
    }

    public <T extends Values> T valuesAs(Class<T> type) {
        return type.cast(values());
    }

    public enum Operator {
        In,
        NotIn
    }
}
