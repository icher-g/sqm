package io.sqm.core.match;

import io.sqm.core.QueryExpr;
import io.sqm.core.RowExpr;
import io.sqm.core.RowListExpr;
import io.sqm.core.ValueSet;

import java.util.function.Function;

/**
 * Default matcher implementation for {@link ValueSet}.
 *
 * @param <R> result type
 */
public class ValueSetMatchImpl<R> implements ValueSetMatch<R> {

    private final ValueSet valueSet;
    private boolean matched = false;
    private R result;

    /**
     * Initializes a match builder for {@link ValueSet}.
     *
     * @param vs value set to match
     */
    public ValueSetMatchImpl(ValueSet vs) {
        this.valueSet = vs;
    }

    /**
     * Registers a handler to be applied when the subject is a {@link RowExpr}.
     *
     * @param f handler for {@code RowExpr}
     * @return {@code this} for fluent chaining
     */
    @Override
    public ValueSetMatch<R> row(Function<RowExpr, R> f) {
        if (!matched && valueSet instanceof RowExpr rowExpr) {
            result = f.apply(rowExpr);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler to be applied when the subject is a {@link RowListExpr}.
     *
     * @param f handler for {@code RowListExpr}
     * @return {@code this} for fluent chaining
     */
    @Override
    public ValueSetMatch<R> rows(Function<RowListExpr, R> f) {
        if (!matched && valueSet instanceof RowListExpr rowListExpr) {
            result = f.apply(rowListExpr);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler to be applied when the subject is a {@link QueryExpr}.
     *
     * @param f handler for {@code QueryExpr}
     * @return {@code this} for fluent chaining
     */
    @Override
    public ValueSetMatch<R> query(Function<QueryExpr, R> f) {
        if (!matched && valueSet instanceof QueryExpr queryExpr) {
            result = f.apply(queryExpr);
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
    public R otherwise(Function<ValueSet, R> f) {
        return matched ? result : f.apply(valueSet);
    }
}
