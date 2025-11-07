package io.sqm.core.match;

import io.sqm.core.OverSpec;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link OverSpec} subtypes.
 * <p>
 * Register handlers for specific over spec derives, then resolve with a terminal method from {@link Match}.
 *
 * @param <R> the result type produced by the match
 */
public interface OverSpecMatch<R> extends Match<OverSpec, R> {
    /**
     * Creates a new matcher for the given {@link OverSpec}.
     *
     * @param os  the over spec to match on
     * @param <R> the result type
     * @return a new {@code OverSpecMatch} for {@code os}
     */
    static <R> OverSpecMatch<R> match(OverSpec os) {
        return new OverSpecMatchImpl<>(os);
    }

    /**
     * Registers a handler for a {@link OverSpec.Ref}.
     *
     * @param f handler for {@code OverSpec.Ref}
     * @return {@code this} for fluent chaining
     */
    OverSpecMatch<R> ref(Function<OverSpec.Ref, R> f);

    /**
     * Registers a handler for a {@link OverSpec.Def}.
     *
     * @param f handler for {@code OverSpec.Def}
     * @return {@code this} for fluent chaining
     */
    OverSpecMatch<R> def(Function<OverSpec.Def, R> f);
}
