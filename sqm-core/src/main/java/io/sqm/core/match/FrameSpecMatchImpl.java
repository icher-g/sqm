package io.sqm.core.match;

import io.sqm.core.FrameSpec;

import java.util.function.Function;

public class FrameSpecMatchImpl<R> implements FrameSpecMatch<R> {

    private final FrameSpec frameSpec;
    private boolean matched = false;
    private R result;

    public FrameSpecMatchImpl(FrameSpec frameSpec) {
        this.frameSpec = frameSpec;
    }

    /**
     * Registers a handler for a {@link FrameSpec.Single}.
     *
     * @param f handler for {@code FrameSpec.Single}
     * @return {@code this} for fluent chaining
     */
    @Override
    public FrameSpecMatch<R> single(Function<FrameSpec.Single, R> f) {
        if (!matched && frameSpec instanceof FrameSpec.Single s) {
            result = f.apply(s);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for a {@link FrameSpec.Between}.
     *
     * @param f handler for {@code FrameSpec.Between}
     * @return {@code this} for fluent chaining
     */
    @Override
    public FrameSpecMatch<R> between(Function<FrameSpec.Between, R> f) {
        if (!matched && frameSpec instanceof FrameSpec.Between b) {
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
    public R otherwise(Function<FrameSpec, R> f) {
        return matched ? result : f.apply(frameSpec);
    }
}
