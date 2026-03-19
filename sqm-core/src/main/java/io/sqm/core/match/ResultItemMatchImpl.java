package io.sqm.core.match;

import io.sqm.core.*;

import java.util.function.Function;

/**
 * Default matcher implementation for {@link ResultItem}.
 *
 * @param <R> result type
 */
public class ResultItemMatchImpl<R> implements ResultItemMatch<R> {

    private final ResultItem item;
    private boolean matched = false;
    private R result;

    /**
     * Initializes a match builder for {@link ResultItem}.
     *
     * @param item result item to match
     */
    public ResultItemMatchImpl(ResultItem item) {
        this.item = item;
    }

    /**
     * Registers a handler for an expression-based result item.
     *
     * @param f handler for expression-based result items
     * @return {@code this} for fluent chaining
     */
    @Override
    public ResultItemMatch<R> expr(Function<ExprResultItem, R> f) {
        if (!matched && item instanceof ExprResultItem i) {
            result = f.apply(i);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for a {@code *} (star) result item.
     *
     * @param f handler for star result items
     * @return {@code this} for fluent chaining
     */
    @Override
    public ResultItemMatch<R> star(Function<StarResultItem, R> f) {
        if (!matched && item instanceof StarResultItem i) {
            result = f.apply(i);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for a qualified {@code t.*} result item.
     *
     * @param f handler for qualified star result items
     * @return {@code this} for fluent chaining
     */
    @Override
    public ResultItemMatch<R> qualifiedStar(Function<QualifiedStarResultItem, R> f) {
        if (!matched && item instanceof QualifiedStarResultItem i) {
            result = f.apply(i);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for a SQL Server pseudo-row-source {@code inserted.*}/{@code deleted.*} result item.
     *
     * @param f handler for output-star result items
     * @return {@code this} for fluent chaining
     */
    @Override
    public ResultItemMatch<R> outputStar(Function<OutputStarResultItem, R> f) {
        if (!matched && item instanceof OutputStarResultItem i) {
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
    public R otherwise(Function<ResultItem, R> f) {
        return matched ? result : f.apply(item);
    }
}
