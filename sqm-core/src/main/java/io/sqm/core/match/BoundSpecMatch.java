package io.sqm.core.match;

import io.sqm.core.BoundSpec;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link BoundSpec} subtypes.
 * <p>
 * Register handlers for specific bound spec derives, then resolve with a terminal method from {@link Match}.
 *
 * @param <R> the result type produced by the match
 */
public interface BoundSpecMatch<R> extends Match<BoundSpec, R> {
    /**
     * Creates a new matcher for the given {@link BoundSpec}.
     *
     * @param bs  the bound spec to match on
     * @param <R> the result type
     * @return a new {@code BoundSpecMatch} for {@code fs}
     */
    static <R> BoundSpecMatch<R> match(BoundSpec bs) {
        return new BoundSpecMatchImpl<>(bs);
    }

    /**
     * Registers a handler for a {@link BoundSpec.Preceding}.
     *
     * @param f handler for {@code BoundSpec.Preceding}
     * @return {@code this} for fluent chaining
     */
    BoundSpecMatch<R> preceding(Function<BoundSpec.Preceding, R> f);

    /**
     * Registers a handler for a {@link BoundSpec.Following}.
     *
     * @param f handler for {@code BoundSpec.Following}
     * @return {@code this} for fluent chaining
     */
    BoundSpecMatch<R> following(Function<BoundSpec.Following, R> f);

    /**
     * Registers a handler for a {@link BoundSpec.CurrentRow}.
     *
     * @param f handler for {@code BoundSpec.CurrentRow}
     * @return {@code this} for fluent chaining
     */
    BoundSpecMatch<R> currentRow(Function<BoundSpec.CurrentRow, R> f);

    /**
     * Registers a handler for a {@link BoundSpec.UnboundedPreceding}.
     *
     * @param f handler for {@code BoundSpec.UnboundedPreceding}
     * @return {@code this} for fluent chaining
     */
    BoundSpecMatch<R> unboundedPreceding(Function<BoundSpec.UnboundedPreceding, R> f);

    /**
     * Registers a handler for a {@link BoundSpec.UnboundedFollowing}.
     *
     * @param f handler for {@code BoundSpec.UnboundedFollowing}
     * @return {@code this} for fluent chaining
     */
    BoundSpecMatch<R> unboundedFollowing(Function<BoundSpec.UnboundedFollowing, R> f);
}
