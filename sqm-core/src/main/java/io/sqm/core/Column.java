package io.sqm.core;

import java.util.List;

/**
 * An interface representing a base for all column statements.
 */
public interface Column extends Entity {
    /**
     * Creates a column that has name/table/alias.
     * For example:
     * {@code
     * Sales.ID AS SID
     * }
     *
     * @param name the name of the column.
     * @return A newly created instance of the column.
     */
    static NamedColumn of(String name) {
        return new NamedColumn(name, null, null);
    }

    /**
     * Creates a column that is represented by a sub query.
     * For example:
     * {@code
     * SELECT ID FROM Sales
     * }
     *
     * @param query a sub query.
     * @return A newly created instance of a column.
     */
    static QueryColumn of(Query query) {
        return new QueryColumn(query, null);
    }

    /**
     * Creates a column that represents a CASE statement.
     * For example:
     * {@code
     * CASE WHEN x = 1 THEN 10 WHEN x = 2 THEN 20 END AS result
     * }
     *
     * @param when a WHEN...THEN statement to start with.
     * @return A newly created instance of a column.
     */
    static CaseColumn of(WhenThen when) {
        return CaseColumn.of(when);
    }

    /**
     * Creates a column that represents a function call.
     * For example:
     * {@code
     * concat('Hello', ', ', 'World') AS greeting
     * }
     *
     * @param name a name of the function.
     * @param args an array of function arguments.
     * @return A newly created instance of a column.
     */
    static FunctionColumn func(String name, FunctionColumn.Arg... args) {
        return new FunctionColumn(name, List.of(args), false, null);
    }

    /**
     * Creates a column that is represented by a free expr. This should be used only if there is no other implementation
     * that can support the requirement.
     *
     * @param exp an expr.
     * @return A newly created instance of a column.
     */
    static ExpressionColumn expr(String exp) {
        return new ExpressionColumn(exp, null);
    }

    /**
     * Creates a column that represents a value.
     * For example: {@code SELECT 1}.
     *
     * @param value a value.
     * @return a newly created instance of a column.
     */
    static ValueColumn val(Object value) {
        return new ValueColumn(value, null);
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#In} op and an array of values.
     *
     * @param items an array of values
     * @return A newly created instance of a filter.
     */
    @SuppressWarnings("unchecked")
    default ColumnFilter in(Object... items) {
        if (items.length == 1 && items[0] instanceof List<?> l) {
            return in((List<Object>) l);
        }
        return in(List.of(items));
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#In} op and a list of values.
     *
     * @param items a list of values
     * @return A newly created instance of a filter.
     */
    default ColumnFilter in(List<Object> items) {
        Values values;
        if (items.size() == 1) values = Values.single(items.get(0));
        else values = Values.list(items);
        return new ColumnFilter(this, ColumnFilter.Operator.In, values);
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#NotIn} op and an array of values.
     *
     * @param items an array of values
     * @return A newly created instance of a filter.
     */
    @SuppressWarnings("unchecked")
    default ColumnFilter notIn(Object... items) {
        if (items.length == 1 && items[0] instanceof List<?> l) {
            return notIn((List<Object>) l);
        }
        return notIn(List.of(items));
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#NotIn} op and a list of values.
     *
     * @param items a list of values
     * @return A newly created instance of a filter.
     */
    default ColumnFilter notIn(List<Object> items) {
        Values values;
        if (items.size() == 1) values = Values.single(items.get(0));
        else values = Values.list(items);
        return new ColumnFilter(this, ColumnFilter.Operator.NotIn, values);
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#Eq} op and a value.
     *
     * @param item a value to compare to.
     * @return A newly created instance of a filter.
     */
    default ColumnFilter eq(Object item) {
        return new ColumnFilter(this, ColumnFilter.Operator.Eq, Values.single(item));
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#Eq} op and a column.
     *
     * @param item a column to compare to.
     * @return A newly created instance of a filter.
     */
    default ColumnFilter eq(Column item) {
        return new ColumnFilter(this, ColumnFilter.Operator.Eq, Values.column(item));
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#Ne} op and a value.
     *
     * @param item a value to compare to.
     * @return A newly created instance of a filter.
     */
    default ColumnFilter ne(Object item) {
        return new ColumnFilter(this, ColumnFilter.Operator.Ne, Values.single(item));
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#Ne} op and a column.
     *
     * @param item a column to compare to.
     * @return A newly created instance of a filter.
     */
    default ColumnFilter ne(Column item) {
        return new ColumnFilter(this, ColumnFilter.Operator.Ne, Values.column(item));
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#Lt} op and a value.
     *
     * @param item a value to compare to.
     * @return A newly created instance of a filter.
     */
    default ColumnFilter lt(Object item) {
        return new ColumnFilter(this, ColumnFilter.Operator.Lt, Values.single(item));
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#Lt} op and a column.
     *
     * @param item a column to compare to.
     * @return A newly created instance of a filter.
     */
    default ColumnFilter lt(Column item) {
        return new ColumnFilter(this, ColumnFilter.Operator.Lt, Values.column(item));
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#Lte} op and a value.
     *
     * @param item a value to compare to.
     * @return A newly created instance of a filter.
     */
    default ColumnFilter lte(Object item) {
        return new ColumnFilter(this, ColumnFilter.Operator.Lte, Values.single(item));
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#Lte} op and a column.
     *
     * @param item a column to compare to.
     * @return A newly created instance of a filter.
     */
    default ColumnFilter lte(Column item) {
        return new ColumnFilter(this, ColumnFilter.Operator.Lte, Values.column(item));
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#Gt} op and a value.
     *
     * @param item a value to compare to.
     * @return A newly created instance of a filter.
     */
    default ColumnFilter gt(Object item) {
        return new ColumnFilter(this, ColumnFilter.Operator.Gt, Values.single(item));
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#Gt} op and a column.
     *
     * @param item a column to compare to.
     * @return A newly created instance of a filter.
     */
    default ColumnFilter gt(Column item) {
        return new ColumnFilter(this, ColumnFilter.Operator.Gt, Values.column(item));
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#Gte} op and a value.
     *
     * @param item a value to compare to.
     * @return A newly created instance of a filter.
     */
    default ColumnFilter gte(Object item) {
        return new ColumnFilter(this, ColumnFilter.Operator.Gte, Values.single(item));
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#Gte} op and a column.
     *
     * @param item a column to compare to.
     * @return A newly created instance of a filter.
     */
    default ColumnFilter gte(Column item) {
        return new ColumnFilter(this, ColumnFilter.Operator.Gte, Values.column(item));
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#Like} op and a string.
     *
     * @param value a string to compare to.
     * @return A newly created instance of a filter.
     */
    default ColumnFilter like(String value) {
        return new ColumnFilter(this, ColumnFilter.Operator.Like, Values.single(value));
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#NotLike} op and a string.
     *
     * @param value a string to compare to.
     * @return A newly created instance of a filter.
     */
    default ColumnFilter notLike(String value) {
        return new ColumnFilter(this, ColumnFilter.Operator.NotLike, Values.single(value));
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#IsNull} op.
     *
     * @return A newly created instance of a filter.
     */
    default ColumnFilter isNull() {
        return new ColumnFilter(this, ColumnFilter.Operator.IsNull, Values.single(null));
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#IsNotNull} op.
     *
     * @return A newly created instance of a filter.
     */
    default ColumnFilter isNotNull() {
        return new ColumnFilter(this, ColumnFilter.Operator.IsNotNull, Values.single(null));
    }

    /**
     * Creates a filter with {@link ColumnFilter.Operator#Range} and min/max values for a BETWEEN statement.
     *
     * @param min a minimum value
     * @param max a maximum value
     * @return A newly created instance of a filter.
     */
    default ColumnFilter range(Object min, Object max) {
        return new ColumnFilter(this, ColumnFilter.Operator.Range, Values.range(min, max));
    }
}
