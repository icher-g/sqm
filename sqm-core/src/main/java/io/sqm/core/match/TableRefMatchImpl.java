package io.sqm.core.match;

import io.sqm.core.*;

import java.util.function.Function;

public class TableRefMatchImpl<R> implements TableRefMatch<R> {

    private final TableRef table;
    private boolean matched = false;
    private R result;

    public TableRefMatchImpl(TableRef table) {
        this.table = table;
    }

    /**
     * Registers a handler for a base {@link Table}.
     *
     * @param f handler for {@code Table}
     * @return {@code this} for fluent chaining
     */
    @Override
    public TableRefMatch<R> table(Function<Table, R> f) {
        if (!matched && table instanceof Table t) {
            result = f.apply(t);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for a {@link QueryTable} (subquery-in-FROM).
     *
     * @param f handler for {@code QueryTable}
     * @return {@code this} for fluent chaining
     */
    @Override
    public TableRefMatch<R> query(Function<QueryTable, R> f) {
        if (!matched && table instanceof QueryTable t) {
            result = f.apply(t);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for a {@link ValuesTable} ({@code VALUES (...)} in {@code FROM}).
     *
     * @param f handler for {@code ValuesTable}
     * @return {@code this} for fluent chaining
     */
    @Override
    public TableRefMatch<R> values(Function<ValuesTable, R> f) {
        if (!matched && table instanceof ValuesTable t) {
            result = f.apply(t);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for a {@link FunctionTable} ({@code f(x)} in {@code FROM}).
     *
     * @param f handler for {@code FunctionTable}
     * @return {@code this} for fluent chaining
     */
    @Override
    public TableRefMatch<R> function(Function<FunctionTable, R> f) {
        if (!matched && table instanceof FunctionTable t) {
            result = f.apply(t);
            matched = true;
        }
        return this;
    }

    /**
     * Matches a {@link Lateral}.
     * <p>
     * This branch is selected when the FROM item is wrapped as lateral,
     * allowing handler logic to explicitly process the wrapped item.
     *
     * @param f the function to apply when the item is a {@link Lateral}
     * @return this matcher for fluent chaining
     */
    @Override
    public TableRefMatch<R> lateral(Function<Lateral, R> f) {
        if (!matched && table instanceof Lateral i) {
            result = f.apply(i);
            matched = true;
        }
        return this;
    }

    /**
     * Terminal operation for this match chain.
     * <p>
     * Executes the first matching branch that was previously registered.
     * If none of the registered type handlers matched the input object,
     * the given fallback function will be applied.
     *
     * @param f a function providing a fallback value if no match occurred
     * @return the computed result, never {@code null} unless produced by the handler
     */
    @Override
    public R otherwise(Function<TableRef, R> f) {
        return matched ? result : f.apply(table);
    }
}
