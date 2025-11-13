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
public class JoinMatchImpl<R> implements JoinMatch<R> {

    private final Join join;
    private boolean matched = false;
    private R result;

    /**
     * Initializes a new instance of {@link JoinMatch}.
     *
     * @param join a join to match.
     */
    public JoinMatchImpl(Join join) {
        this.join = join;
    }

    /**
     * Registers a handler for {@link OnJoin}.
     *
     * @param f handler for {@code OnJoin}
     * @return {@code this} for fluent chaining
     */
    @Override
    public JoinMatch<R> on(Function<OnJoin, R> f) {
        if (!matched && join instanceof OnJoin j) {
            result = f.apply(j);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for {@link UsingJoin}.
     *
     * @param f handler for {@code UsingJoin}
     * @return {@code this} for fluent chaining
     */
    @Override
    public JoinMatch<R> using(Function<UsingJoin, R> f) {
        if (!matched && join instanceof UsingJoin j) {
            result = f.apply(j);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for {@link NaturalJoin}.
     *
     * @param f handler for {@code NaturalJoin}
     * @return {@code this} for fluent chaining
     */
    @Override
    public JoinMatch<R> natural(Function<NaturalJoin, R> f) {
        if (!matched && join instanceof NaturalJoin j) {
            result = f.apply(j);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for {@link CrossJoin}.
     *
     * @param f handler for {@code CrossJoin}
     * @return {@code this} for fluent chaining
     */
    @Override
    public JoinMatch<R> cross(Function<CrossJoin, R> f) {
        if (!matched && join instanceof CrossJoin j) {
            result = f.apply(j);
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
    public R otherwise(Function<Join, R> f) {
        return matched ? result : f.apply(join);
    }
}
