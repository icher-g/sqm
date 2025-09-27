package io.cherlabs.sqlmodel.core;

import io.cherlabs.sqlmodel.core.traits.HasColumn;
import io.cherlabs.sqlmodel.core.traits.HasColumnOperator;
import io.cherlabs.sqlmodel.core.traits.HasValues;

import java.util.List;

public record ColumnFilter(Column column, Operator operator, Values values) implements Filter, HasColumn, HasColumnOperator, HasValues {
    public enum Operator {
        In,
        NotIn,
        Range,
        Eq,
        Ne,
        Lt,
        Lte,
        Gt,
        Gte,
        Like,
        NotLike,
        IsNull,
        IsNotNull
    }

    @SuppressWarnings("unchecked")
    public ColumnFilter in(Object... items) {
        if (items.length == 1 && items[0] instanceof List<?> l) {
            return in((List<Object>) l);
        }
        return in(List.of(items));
    }

    public ColumnFilter in(List<Object> items) {
        Values values;
        if (items.size() == 1) values = Values.single(items.get(0));
        else values = Values.list(items);
        return new ColumnFilter(column, Operator.In, values);
    }

    @SuppressWarnings("unchecked")
    public ColumnFilter notIn(Object... items) {
        if (items.length == 1 && items[0] instanceof List<?> l) {
            return notIn((List<Object>) l);
        }
        return notIn(List.of(items));
    }

    public ColumnFilter notIn(List<Object> items) {
        Values values;
        if (items.size() == 1) values = Values.single(items.get(0));
        else values = Values.list(items);
        return new ColumnFilter(column, Operator.NotIn, values);
    }

    public ColumnFilter eq(Object item) {
        return new ColumnFilter(column, Operator.Eq, Values.single(item));
    }

    public ColumnFilter eq(Column item) {
        return new ColumnFilter(column, Operator.Eq, Values.column(item));
    }

    public ColumnFilter ne(Object item) {
        return new ColumnFilter(column, Operator.Ne, Values.single(item));
    }

    public ColumnFilter ne(Column item) {
        return new ColumnFilter(column, Operator.Ne, Values.column(item));
    }

    public ColumnFilter lt(Object item) {
        return new ColumnFilter(column, Operator.Lt, Values.single(item));
    }

    public ColumnFilter lt(Column item) {
        return new ColumnFilter(column, Operator.Lt, Values.column(item));
    }

    public ColumnFilter lte(Object item) {
        return new ColumnFilter(column, Operator.Lte, Values.single(item));
    }

    public ColumnFilter lte(Column item) {
        return new ColumnFilter(column, Operator.Lte, Values.column(item));
    }

    public ColumnFilter gt(Object item) {
        return new ColumnFilter(column, Operator.Gt, Values.single(item));
    }

    public ColumnFilter gt(Column item) {
        return new ColumnFilter(column, Operator.Gt, Values.column(item));
    }

    public ColumnFilter gte(Object item) {
        return new ColumnFilter(column, Operator.Gte, Values.single(item));
    }

    public ColumnFilter gte(Column item) {
        return new ColumnFilter(column, Operator.Gte, Values.column(item));
    }

    public ColumnFilter like(String value) {
        return new ColumnFilter(column, Operator.Like, Values.single(value));
    }

    public ColumnFilter notLike(String value) {
        return new ColumnFilter(column, Operator.NotLike, Values.single(value));
    }

    public ColumnFilter isNull() {
        return new ColumnFilter(column, Operator.IsNull, Values.single(null));
    }

    public ColumnFilter isNotNull() {
        return new ColumnFilter(column, Operator.IsNotNull, Values.single(null));
    }

    public ColumnFilter range(Object min, Object max) {
        return new ColumnFilter(column, Operator.Range, Values.range(min, max));
    }

    public <T extends Column> T columnAs(Class<T> type) {
        return type.cast(column());
    }

    public <T extends Values> T valuesAs(Class<T> type) {
        return type.cast(values());
    }
}
