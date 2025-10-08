package io.cherlabs.sqm.core;

import io.cherlabs.sqm.core.traits.HasColumn;
import io.cherlabs.sqm.core.traits.HasColumnOperator;
import io.cherlabs.sqm.core.traits.HasValues;

import java.util.List;

/**
 * Represents a filter that has a column on its left side an op in the middle and a set of possible values on the right.
 * For example:
 * {@code
 * c <> 7
 * c BETWEEN 5 AND 10
 * c IS NULL
 * c IN (1, 2, 3)
 * etc...
 * }
 *
 * @param column   a column
 * @param op an op
 * @param values   a value(s)
 */
public record ColumnFilter(Column column, Operator op, Values values) implements Filter, HasColumn, HasColumnOperator, HasValues {
    /**
     * Creates a filter with {@link Operator#In} op and an array of values.
     *
     * @param items an array of values
     * @return A newly created instance of a filter.
     */
    @SuppressWarnings("unchecked")
    public ColumnFilter in(Object... items) {
        if (items.length == 1 && items[0] instanceof List<?> l) {
            return in((List<Object>) l);
        }
        return in(List.of(items));
    }

    /**
     * Creates a filter with {@link Operator#In} op and a list of values.
     *
     * @param items a list of values
     * @return A newly created instance of a filter.
     */
    public ColumnFilter in(List<Object> items) {
        Values values;
        if (items.size() == 1) values = Values.single(items.get(0));
        else values = Values.list(items);
        return new ColumnFilter(column, Operator.In, values);
    }

    /**
     * Creates a filter with {@link Operator#NotIn} op and an array of values.
     *
     * @param items an array of values
     * @return A newly created instance of a filter.
     */
    @SuppressWarnings("unchecked")
    public ColumnFilter notIn(Object... items) {
        if (items.length == 1 && items[0] instanceof List<?> l) {
            return notIn((List<Object>) l);
        }
        return notIn(List.of(items));
    }

    /**
     * Creates a filter with {@link Operator#NotIn} op and a list of values.
     *
     * @param items a list of values
     * @return A newly created instance of a filter.
     */
    public ColumnFilter notIn(List<Object> items) {
        Values values;
        if (items.size() == 1) values = Values.single(items.get(0));
        else values = Values.list(items);
        return new ColumnFilter(column, Operator.NotIn, values);
    }

    /**
     * Creates a filter with {@link Operator#Eq} op and a value.
     *
     * @param item a value to compare to.
     * @return A newly created instance of a filter.
     */
    public ColumnFilter eq(Object item) {
        return new ColumnFilter(column, Operator.Eq, Values.single(item));
    }

    /**
     * Creates a filter with {@link Operator#Eq} op and a column.
     *
     * @param item a column to compare to.
     * @return A newly created instance of a filter.
     */
    public ColumnFilter eq(Column item) {
        return new ColumnFilter(column, Operator.Eq, Values.column(item));
    }

    /**
     * Creates a filter with {@link Operator#Ne} op and a value.
     *
     * @param item a value to compare to.
     * @return A newly created instance of a filter.
     */
    public ColumnFilter ne(Object item) {
        return new ColumnFilter(column, Operator.Ne, Values.single(item));
    }

    /**
     * Creates a filter with {@link Operator#Ne} op and a column.
     *
     * @param item a column to compare to.
     * @return A newly created instance of a filter.
     */
    public ColumnFilter ne(Column item) {
        return new ColumnFilter(column, Operator.Ne, Values.column(item));
    }

    /**
     * Creates a filter with {@link Operator#Lt} op and a value.
     *
     * @param item a value to compare to.
     * @return A newly created instance of a filter.
     */
    public ColumnFilter lt(Object item) {
        return new ColumnFilter(column, Operator.Lt, Values.single(item));
    }

    /**
     * Creates a filter with {@link Operator#Lt} op and a column.
     *
     * @param item a column to compare to.
     * @return A newly created instance of a filter.
     */
    public ColumnFilter lt(Column item) {
        return new ColumnFilter(column, Operator.Lt, Values.column(item));
    }

    /**
     * Creates a filter with {@link Operator#Lte} op and a value.
     *
     * @param item a value to compare to.
     * @return A newly created instance of a filter.
     */
    public ColumnFilter lte(Object item) {
        return new ColumnFilter(column, Operator.Lte, Values.single(item));
    }

    /**
     * Creates a filter with {@link Operator#Lte} op and a column.
     *
     * @param item a column to compare to.
     * @return A newly created instance of a filter.
     */
    public ColumnFilter lte(Column item) {
        return new ColumnFilter(column, Operator.Lte, Values.column(item));
    }

    /**
     * Creates a filter with {@link Operator#Gt} op and a value.
     *
     * @param item a value to compare to.
     * @return A newly created instance of a filter.
     */
    public ColumnFilter gt(Object item) {
        return new ColumnFilter(column, Operator.Gt, Values.single(item));
    }

    /**
     * Creates a filter with {@link Operator#Gt} op and a column.
     *
     * @param item a column to compare to.
     * @return A newly created instance of a filter.
     */
    public ColumnFilter gt(Column item) {
        return new ColumnFilter(column, Operator.Gt, Values.column(item));
    }

    /**
     * Creates a filter with {@link Operator#Gte} op and a value.
     *
     * @param item a value to compare to.
     * @return A newly created instance of a filter.
     */
    public ColumnFilter gte(Object item) {
        return new ColumnFilter(column, Operator.Gte, Values.single(item));
    }

    /**
     * Creates a filter with {@link Operator#Gte} op and a column.
     *
     * @param item a column to compare to.
     * @return A newly created instance of a filter.
     */
    public ColumnFilter gte(Column item) {
        return new ColumnFilter(column, Operator.Gte, Values.column(item));
    }

    /**
     * Creates a filter with {@link Operator#Like} op and a string.
     *
     * @param value a string to compare to.
     * @return A newly created instance of a filter.
     */
    public ColumnFilter like(String value) {
        return new ColumnFilter(column, Operator.Like, Values.single(value));
    }

    /**
     * Creates a filter with {@link Operator#NotLike} op and a string.
     *
     * @param value a string to compare to.
     * @return A newly created instance of a filter.
     */
    public ColumnFilter notLike(String value) {
        return new ColumnFilter(column, Operator.NotLike, Values.single(value));
    }

    /**
     * Creates a filter with {@link Operator#IsNull} op.
     *
     * @return A newly created instance of a filter.
     */
    public ColumnFilter isNull() {
        return new ColumnFilter(column, Operator.IsNull, Values.single(null));
    }

    /**
     * Creates a filter with {@link Operator#IsNotNull} op.
     *
     * @return A newly created instance of a filter.
     */
    public ColumnFilter isNotNull() {
        return new ColumnFilter(column, Operator.IsNotNull, Values.single(null));
    }

    /**
     * Creates a filter with {@link Operator#Range} and min/max values for a BETWEEN statement.
     *
     * @param min a minimum value
     * @param max a maximum value
     * @return A newly created instance of a filter.
     */
    public ColumnFilter range(Object min, Object max) {
        return new ColumnFilter(column, Operator.Range, Values.range(min, max));
    }

    /**
     * Returns a column and casts it to a specific type.
     *
     * @param type the type to cast a column to.
     * @param <T>  The type of the column to be returned.
     * @return a colum of the specified type.
     */
    public <T extends Column> T columnAs(Class<T> type) {
        return type.cast(column());
    }

    /**
     * Returns {@link Values} and casts to a specific type.
     *
     * @param type the type to cast the values to.
     * @param <T>  The type of the Values to be returned.
     * @return a Values of the specified type.
     */
    public <T extends Values> T valuesAs(Class<T> type) {
        return type.cast(values());
    }

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
}
