package io.sqm.core.match;

import io.sqm.core.ExprSelectItem;
import io.sqm.core.QualifiedStarSelectItem;
import io.sqm.core.SelectItem;
import io.sqm.core.StarSelectItem;

import java.util.function.Function;

/**
 * Default matcher implementation for {@link SelectItem}.
 *
 * @param <R> result type
 */
public class SelectItemMatchImpl<R> implements SelectItemMatch<R> {

    private final SelectItem item;
    private boolean matched = false;
    private R result;

    /**
     * Initializes a match builder for {@link SelectItem}.
     *
     * @param item select item to match
     */
    public SelectItemMatchImpl(SelectItem item) {
        this.item = item;
    }

    /**
     * Registers a handler for an expression-based select item.
     *
     * @param f handler for expression-based select items
     * @return {@code this} for fluent chaining
     */
    @Override
    public SelectItemMatch<R> expr(Function<ExprSelectItem, R> f) {
        if (!matched && item instanceof ExprSelectItem i) {
            result = f.apply(i);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for a {@code *} (star) select item.
     *
     * @param f handler for star select items
     * @return {@code this} for fluent chaining
     */
    @Override
    public SelectItemMatch<R> star(Function<StarSelectItem, R> f) {
        if (!matched && item instanceof StarSelectItem i) {
            result = f.apply(i);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for a qualified {@code t.*} select item.
     *
     * @param f handler for qualified star select items
     * @return {@code this} for fluent chaining
     */
    @Override
    public SelectItemMatch<R> qualifiedStar(Function<QualifiedStarSelectItem, R> f) {
        if (!matched && item instanceof QualifiedStarSelectItem i) {
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
    public R otherwise(Function<SelectItem, R> f) {
        return matched ? result : f.apply(item);
    }
}
