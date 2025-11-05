package io.sqm.core.match;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.WithQuery;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link Query} subtypes.
 * <p>
 * Register handlers for query kinds (expr.g., {@link SelectQuery}, {@link WithQuery}),
 * then resolve with a terminal method from {@link Match}.
 *
 * @param <R> the result type produced by the match
 */
public interface QueryMatch<R> extends Match<Query, R> {

    /**
     * Creates a new matcher for the given {@link Query}.
     *
     * @param q   the query to match on
     * @param <R> the result type
     * @return a new {@code QueryMatch} for {@code q}
     */
    static <R> QueryMatch<R> match(Query q) {
        return new QueryMatchImpl<>(q);
    }

    /**
     * Registers a handler for {@link SelectQuery}.
     *
     * @param f handler for {@code SelectQuery}
     * @return {@code this} for fluent chaining
     */
    QueryMatch<R> select(Function<SelectQuery, R> f);

    /**
     * Registers a handler for {@link WithQuery}.
     *
     * @param f handler for {@code WithQuery}
     * @return {@code this} for fluent chaining
     */
    QueryMatch<R> with(Function<WithQuery, R> f);

    /**
     * Registers a handler for {@link CompositeQuery}.
     *
     * @param f handler for {@code CompositeQuery}
     * @return {@code this} for fluent chaining
     */
    QueryMatch<R> composite(Function<CompositeQuery, R> f);
}


