package io.sqm.core.match;

import io.sqm.core.*;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link ResultItem} subtypes.
 * <p>
 * Register handlers for item kinds (expression-based, {@code *}, or qualified {@code t.*}),
 * then resolve with a terminal method from {@link Match}.
 *
 * @param <R> the result type produced by the match
 */
public interface ResultItemMatch<R> extends Match<ResultItem, R> {

    /**
     * Creates a new matcher for the given {@link ResultItem}.
     *
     * @param i   the result item to match on
     * @param <R> the result type
     * @return a new {@code ResultItemMatch} for {@code i}
     */
    static <R> ResultItemMatch<R> match(ResultItem i) {
        return new ResultItemMatchImpl<>(i);
    }

    /**
     * Registers a handler for an expression-based result item.
     *
     * @param f handler for expression-based result items
     * @return {@code this} for fluent chaining
     */
    ResultItemMatch<R> expr(Function<ExprResultItem, R> f);

    /**
     * Registers a handler for a {@code *} (star) result item.
     *
     * @param f handler for star result items
     * @return {@code this} for fluent chaining
     */
    ResultItemMatch<R> star(Function<StarResultItem, R> f);

    /**
     * Registers a handler for a qualified {@code t.*} result item.
     *
     * @param f handler for qualified star result items
     * @return {@code this} for fluent chaining
     */
    ResultItemMatch<R> qualifiedStar(Function<QualifiedStarResultItem, R> f);

    /**
     * Registers a handler for a SQL Server pseudo-row-source {@code inserted.*}/{@code deleted.*} result item.
     *
     * @param f handler for output-star result items
     * @return {@code this} for fluent chaining
     */
    ResultItemMatch<R> outputStar(Function<OutputStarResultItem, R> f);
}
