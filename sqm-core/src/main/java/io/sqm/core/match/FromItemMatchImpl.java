package io.sqm.core.match;

import io.sqm.core.FromItem;
import io.sqm.core.Join;
import io.sqm.core.TableRef;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link FromItem} subtypes.
 * <p>
 * Register handlers for specific join kinds, then resolve with a terminal method from {@link Match}.
 *
 * @param <R> the result type produced by the match
 */
public class FromItemMatchImpl<R> implements FromItemMatch<R> {

    private final FromItem item;
    private boolean matched = false;
    private R result;

    /**
     * Initializes a new instance of {@link FromItemMatch}.
     *
     * @param item a join to match.
     */
    public FromItemMatchImpl(FromItem item) {
        this.item = item;
    }

    /**
     * Matches a {@link Join}.
     *
     * @param f the function to apply when the item is a {@link Join}
     * @return this matcher for fluent chaining
     */
    @Override
    public FromItemMatch<R> join(Function<Join, R> f) {
        if (!matched && item instanceof Join j) {
            result = f.apply(j);
            matched = true;
        }
        return this;
    }

    /**
     * Matches a {@link TableRef}.
     *
     * @param f the function to apply when the item is a {@link TableRef}
     * @return this matcher for fluent chaining
     */
    @Override
    public FromItemMatch<R> tableRef(Function<TableRef, R> f) {
        if (!matched && item instanceof TableRef t) {
            result = f.apply(t);
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
    public R otherwise(Function<FromItem, R> f) {
        return matched ? result : f.apply(item);
    }
}
