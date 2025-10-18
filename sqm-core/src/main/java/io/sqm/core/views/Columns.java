package io.sqm.core.views;

import io.sqm.core.*;
import io.sqm.core.traits.*;

import java.util.List;
import java.util.Optional;

/**
 * A view that provides access to {@link Column} properties.
 */
public final class Columns {
    private Columns() {
    }

    /**
     * Gets the name of the column from the derived type if it is presented.
     *
     * @param c a reference to a column.
     * @return {@link Optional} with the column name if presented or an {@link Optional#empty()}.
     */
    public static Optional<String> name(Column c) {
        if (c instanceof HasName h) return Optional.ofNullable(h.name());
        return Optional.empty();
    }

    /**
     * Gets the alias of the column from the derived type if it is presented.
     *
     * @param c a reference to a column.
     * @return {@link Optional} with the column alias if presented or an {@link Optional#empty()}.
     */
    public static Optional<String> alias(Column c) {
        if (c instanceof HasAlias h) return Optional.ofNullable(h.alias());
        return Optional.empty();
    }

    /**
     * Gets the table of the column from the derived type if it is presented.
     *
     * @param c a reference to a column.
     * @return {@link Optional} with the column table if presented or an {@link Optional#empty()}.
     */
    public static Optional<String> table(Column c) {
        if (c instanceof HasTableName h) return Optional.ofNullable(h.table());
        return Optional.empty();
    }

    /**
     * Gets the expr of the column from the derived type if it is presented.
     *
     * @param c a reference to a column.
     * @return {@link Optional} with the column expr if presented or an {@link Optional#empty()}.
     */
    public static Optional<String> expr(Column c) {
        if (c instanceof HasExpr h) return Optional.ofNullable(h.expr());
        return Optional.empty();
    }

    /**
     * Gets the sub query from the derived type if it is presented.
     *
     * @param c a reference to a column.
     * @return {@link Optional} with the sub query if presented or an {@link Optional#empty()}.
     */
    public static Optional<Query> query(Column c) {
        if (c instanceof HasQuery h) return Optional.ofNullable(h.query());
        return Optional.empty();
    }

    /**
     * Gets the list of the arguments from the derived type if it is presented.
     *
     * @param c a reference to a column.
     * @return {@link Optional} with the list of the arguments if presented or an {@link Optional#empty()}.
     */
    public static Optional<List<FunctionColumn.Arg>> funcArgs(Column c) {
        if (c instanceof HasArgs h) return Optional.ofNullable(h.args());
        return Optional.empty();
    }

    /**
     * Gets the distinct value from the derived type if it is presented.
     *
     * @param c a reference to a column.
     * @return {@link Optional} with the distinct value if presented or an {@link Optional#empty()}.
     */
    public static Optional<Boolean> distinct(Column c) {
        if (c instanceof HasDistinct h) return Optional.ofNullable(h.distinct());
        return Optional.empty();
    }

    /**
     * Gets when...then expressions of the column from the derived type if it is presented.
     *
     * @param c a reference to a column.
     * @return {@link Optional} with the column when...then expressions if presented or an {@link Optional#empty()}.
     */
    public static Optional<List<WhenThen>> whens(Column c) {
        if (c instanceof HasWhens h) return Optional.of(h.whens());
        return Optional.empty();
    }

    /**
     * Gets the else value expr of the column from the derived type if it is presented.
     *
     * @param c a reference to a column.
     * @return {@link Optional} with the column else value expr if presented or an {@link Optional#empty()}.
     */
    public static Optional<Entity> elseValue(Column c) {
        if (c instanceof HasElseValue h) return Optional.of(h.elseValue());
        return Optional.empty();
    }
}
