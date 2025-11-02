package io.sqm.core.match;

import io.sqm.core.*;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link Join} subtypes.
 * <p>
 * Register handlers for specific join kinds, then resolve with a terminal method from {@link Match}.
 *
 * @param <R> the result type produced by the match
 */
public interface JoinMatch<R> extends Match<Join, R> {

    /**
     * Creates a new matcher for the given {@link Join}.
     *
     * @param j   the join to match on
     * @param <R> the result type
     * @return a new {@code JoinMatch} for {@code j}
     */
    static <R> JoinMatch<R> match(Join j) {
        return new JoinMatchImpl<>(j);
    }

    /**
     * Registers a handler for {@link OnJoin}.
     *
     * @param f handler for {@code OnJoin}
     * @return {@code this} for fluent chaining
     */
    JoinMatch<R> on(Function<OnJoin, R> f);

    /**
     * Registers a handler for {@link UsingJoin}.
     *
     * @param f handler for {@code UsingJoin}
     * @return {@code this} for fluent chaining
     */
    JoinMatch<R> using(Function<UsingJoin, R> f);

    /**
     * Registers a handler for {@link NaturalJoin}.
     *
     * @param f handler for {@code NaturalJoin}
     * @return {@code this} for fluent chaining
     */
    JoinMatch<R> natural(Function<NaturalJoin, R> f);

    /**
     * Registers a handler for {@link CrossJoin}.
     *
     * @param f handler for {@code CrossJoin}
     * @return {@code this} for fluent chaining
     */
    JoinMatch<R> cross(Function<CrossJoin, R> f);
}


