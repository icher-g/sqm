package io.sqm.core.match;

import io.sqm.core.*;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link TableRef} subtypes.
 * <p>
 * Register handlers for table reference kinds (base table, subquery, {@code VALUES} table),
 * then resolve with a terminal method from {@link Match}.
 *
 * @param <R> the result type produced by the match
 */
public interface TableRefMatch<R> extends Match<TableRef, R> {

    /**
     * Creates a new matcher for the given {@link TableRef}.
     *
     * @param t   the table reference to match on
     * @param <R> the result type
     * @return a new {@code TableMatch} for {@code t}
     */
    static <R> TableRefMatch<R> match(TableRef t) {
        return new TableRefMatchImpl<>(t);
    }

    /**
     * Registers a handler for a base {@link Table}.
     *
     * @param f handler for {@code Table}
     * @return {@code this} for fluent chaining
     */
    TableRefMatch<R> table(Function<Table, R> f);

    /**
     * Registers a handler for a {@link QueryTable} (subquery-in-FROM).
     *
     * @param f handler for {@code QueryTable}
     * @return {@code this} for fluent chaining
     */
    TableRefMatch<R> query(Function<QueryTable, R> f);

    /**
     * Registers a handler for a {@link ValuesTable} ({@code VALUES (...)} in {@code FROM}).
     *
     * @param f handler for {@code ValuesTable}
     * @return {@code this} for fluent chaining
     */
    TableRefMatch<R> values(Function<ValuesTable, R> f);

    /**
     * Registers a handler for a {@link FunctionTable} ({@code f(x)} in {@code FROM}).
     *
     * @param f handler for {@code FunctionTable}
     * @return {@code this} for fluent chaining
     */
    TableRefMatch<R> function(Function<FunctionTable, R> f);

    /**
     * Matches a {@link Lateral}.
     * <p>
     * This branch is selected when the FROM item is wrapped as lateral,
     * allowing handler logic to explicitly process the wrapped item.
     *
     * @param f the function to apply when the item is a {@link Lateral}
     * @return this matcher for fluent chaining
     */
    TableRefMatch<R> lateral(Function<Lateral, R> f);
}


