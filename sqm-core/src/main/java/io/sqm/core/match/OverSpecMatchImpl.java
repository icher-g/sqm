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
public class OverSpecMatchImpl<R> implements OverSpecMatch<R> {

    private final OverSpec overSpec;
    private boolean matched = false;
    private R result;

    /**
     * Initializes a match builder for {@link OverSpec}.
     *
     * @param overSpec over spec to match
     */
    public OverSpecMatchImpl(OverSpec overSpec) {
        this.overSpec = overSpec;
    }

    /**
     * Registers a handler for a {@link OverSpec.Ref}.
     *
     * @param f handler for {@code OverSpec.Ref}
     * @return {@code this} for fluent chaining
     */
    @Override
    public OverSpecMatch<R> ref(Function<OverSpec.Ref, R> f) {
        if (!matched && overSpec instanceof OverSpec.Ref ref) {
            result = f.apply(ref);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for a {@link OverSpec.Def}.
     *
     * @param f handler for {@code OverSpec.Def}
     * @return {@code this} for fluent chaining
     */
    @Override
    public OverSpecMatch<R> def(Function<OverSpec.Def, R> f) {
        if (!matched && overSpec instanceof OverSpec.Def def) {
            result = f.apply(def);
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
    public R otherwise(Function<OverSpec, R> f) {
        return matched ? result : f.apply(overSpec);
    }
}
