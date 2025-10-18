package io.cherlabs.sqm.core.views;

import io.cherlabs.sqm.core.*;
import io.cherlabs.sqm.core.traits.*;

import java.util.List;
import java.util.Optional;

/**
 * A view that provides access to {@link Query} properties.
 */
public class Queries {

    /**
     * Gets the body query implemented by one of the derived types if presented.
     *
     * @param q the query to retrieve the property from.
     * @return {@link Optional} with the query body if presented or an {@link Optional#empty()}.
     */
    public static Optional<Query> body(Query q) {
        if (q instanceof HasBody h) return Optional.ofNullable(h.body());
        return Optional.empty();
    }

    /**
     * Gets the recursive value implemented by one of the derived types if presented.
     *
     * @param q the query to retrieve the property from.
     * @return {@link Optional} with the recursive value if presented or an {@link Optional#empty()}.
     */
    public static Optional<Boolean> recursive(Query q) {
        if (q instanceof HasRecursive h) return Optional.of(h.recursive());
        return Optional.empty();
    }

    /**
     * Gets the list of column aliases (mostly used by the CTE queries) implemented by one of the derived types if presented.
     *
     * @param q the query to retrieve the property from.
     * @return {@link Optional} with the list of column aliases if presented or an {@link Optional#empty()}.
     */
    public static Optional<List<String>> columnAliases(Query q) {
        if (q instanceof HasColumnAliases h) return Optional.ofNullable(h.columnAliases());
        return Optional.empty();
    }

    /**
     * Gets the body query implemented by one of the derived types if presented.
     *
     * @param q the query to retrieve the property from.
     * @return {@link Optional} with the query body if presented or an {@link Optional#empty()}.
     */
    public static Optional<List<CteQuery>> ctes(Query q) {
        if (q instanceof HasCtes h) return Optional.ofNullable(h.ctes());
        return Optional.empty();
    }

    /**
     * Gets a limit implemented by one of the derived types if presented.
     *
     * @param q the query to retrieve the property from.
     * @return {@link Optional} with a limit value if presented or an {@link Optional#empty()}.
     */
    public static Optional<Long> limit(Query q) {
        if (q instanceof HasLimit h) return Optional.ofNullable(h.limit());
        return Optional.empty();
    }

    /**
     * Gets an offset implemented by one of the derived types if presented.
     *
     * @param q the query to retrieve the property from.
     * @return {@link Optional} with an offset value if presented or an {@link Optional#empty()}.
     */
    public static Optional<Long> offset(Query q) {
        if (q instanceof HasOffset h) return Optional.ofNullable(h.offset());
        return Optional.empty();
    }

    /**
     * Gets a distinct implemented by one of the derived types if presented.
     *
     * @param q the query to retrieve the property from.
     * @return {@link Optional} with the distinct value if presented or an {@link Optional#empty()}.
     */
    public static Optional<Boolean> distinct(Query q) {
        if (q instanceof HasDistinct h) return Optional.ofNullable(h.distinct());
        return Optional.empty();
    }

    /**
     * Gets a list of columns implemented by one of the derived types if presented.
     *
     * @param q the query to retrieve the property from.
     * @return {@link Optional} with the list of columns if presented or an {@link Optional#empty()}.
     */
    public static Optional<List<Column>> columns(Query q) {
        if (q instanceof HasColumns h) return Optional.ofNullable(h.columns());
        return Optional.empty();
    }

    /**
     * Gets a table implemented by one of the derived types if presented.
     *
     * @param q the query to retrieve the property from.
     * @return {@link Optional} with the table value if presented or an {@link Optional#empty()}.
     */
    public static Optional<Table> table(Query q) {
        if (q instanceof HasTable h) return Optional.ofNullable(h.table());
        return Optional.empty();
    }

    /**
     * Gets a list of joins implemented by one of the derived types if presented.
     *
     * @param q the query to retrieve the property from.
     * @return {@link Optional} with the list of joins if presented or an {@link Optional#empty()}.
     */
    public static Optional<List<Join>> joins(Query q) {
        if (q instanceof HasJoins h) return Optional.ofNullable(h.joins());
        return Optional.empty();
    }

    /**
     * Gets a list of group by items implemented by one of the derived types if presented.
     *
     * @param q the query to retrieve the property from.
     * @return {@link Optional} with the list of group by items if presented or an {@link Optional#empty()}.
     */
    public static Optional<GroupBy> groupBy(Query q) {
        if (q instanceof HasGroupBy h) return Optional.ofNullable(h.groupBy());
        return Optional.empty();
    }

    /**
     * Gets a list of order by items implemented by one of the derived types if presented.
     *
     * @param q the query to retrieve the property from.
     * @return {@link Optional} with the list of order by items if presented or an {@link Optional#empty()}.
     */
    public static Optional<OrderBy> orderBy(Query q) {
        if (q instanceof HasOrderBy h) return Optional.ofNullable(h.orderBy());
        return Optional.empty();
    }

    /**
     * Gets a where statement implemented by one of the derived types if presented.
     *
     * @param q the query to retrieve the property from.
     * @return {@link Optional} with the where statement if presented or an {@link Optional#empty()}.
     */
    public static Optional<Filter> where(Query q) {
        if (q instanceof HasWhere h) return Optional.ofNullable(h.where());
        return Optional.empty();
    }

    /**
     * Gets a having statement implemented by one of the derived types if presented.
     *
     * @param q the query to retrieve the property from.
     * @return {@link Optional} with the having statement if presented or an {@link Optional#empty()}.
     */
    public static Optional<Filter> having(Query q) {
        if (q instanceof HasHaving h) return Optional.ofNullable(h.having());
        return Optional.empty();
    }

    /**
     * Gets a list of terms (sub queries) implemented by one of the derived types if presented.
     *
     * @param q the query to retrieve the property from.
     * @return {@link Optional} with the list of terms if presented or an {@link Optional#empty()}.
     */
    public static Optional<List<Query>> terms(Query q) {
        if (q instanceof HasTerms h) return Optional.ofNullable(h.terms());
        return Optional.empty();
    }

    /**
     * Gets a list of operations to be applied on the terms (sub queries) implemented by one of the derived types if presented.
     *
     * @param q the query to retrieve the property from.
     * @return {@link Optional} with the list of operations if presented or an {@link Optional#empty()}.
     */
    public static Optional<List<CompositeQuery.Op>> ops(Query q) {
        if (q instanceof HasOps h) return Optional.ofNullable(h.ops());
        return Optional.empty();
    }
}
