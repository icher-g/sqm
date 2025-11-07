package io.sqm.core.match;

import io.sqm.core.BoundSpec;

import java.util.function.Function;

public class BoundSpecMatchImpl<R> implements BoundSpecMatch<R> {

    private final BoundSpec boundSpec;
    private boolean matched = false;
    private R result;

    public BoundSpecMatchImpl(BoundSpec boundSpec) {
        this.boundSpec = boundSpec;
    }

    /**
     * Registers a handler for a {@link BoundSpec.Preceding}.
     *
     * @param f handler for {@code BoundSpec.Preceding}
     * @return {@code this} for fluent chaining
     */
    @Override
    public BoundSpecMatch<R> preceding(Function<BoundSpec.Preceding, R> f) {
        if (!matched && boundSpec instanceof BoundSpec.Preceding b) {
            result = f.apply(b);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for a {@link BoundSpec.Following}.
     *
     * @param f handler for {@code BoundSpec.Following}
     * @return {@code this} for fluent chaining
     */
    @Override
    public BoundSpecMatch<R> following(Function<BoundSpec.Following, R> f) {
        if (!matched && boundSpec instanceof BoundSpec.Following b) {
            result = f.apply(b);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for a {@link BoundSpec.CurrentRow}.
     *
     * @param f handler for {@code BoundSpec.CurrentRow}
     * @return {@code this} for fluent chaining
     */
    @Override
    public BoundSpecMatch<R> currentRow(Function<BoundSpec.CurrentRow, R> f) {
        if (!matched && boundSpec instanceof BoundSpec.CurrentRow b) {
            result = f.apply(b);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for a {@link BoundSpec.UnboundedPreceding}.
     *
     * @param f handler for {@code BoundSpec.UnboundedPreceding}
     * @return {@code this} for fluent chaining
     */
    @Override
    public BoundSpecMatch<R> unboundedPreceding(Function<BoundSpec.UnboundedPreceding, R> f) {
        if (!matched && boundSpec instanceof BoundSpec.UnboundedPreceding b) {
            result = f.apply(b);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for a {@link BoundSpec.UnboundedFollowing}.
     *
     * @param f handler for {@code BoundSpec.UnboundedFollowing}
     * @return {@code this} for fluent chaining
     */
    @Override
    public BoundSpecMatch<R> unboundedFollowing(Function<BoundSpec.UnboundedFollowing, R> f) {
        if (!matched && boundSpec instanceof BoundSpec.UnboundedFollowing b) {
            result = f.apply(b);
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
    public R otherwise(Function<BoundSpec, R> f) {
        return matched ? result : f.apply(boundSpec);
    }
}
