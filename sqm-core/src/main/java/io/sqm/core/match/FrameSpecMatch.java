package io.sqm.core.match;

import io.sqm.core.FrameSpec;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link FrameSpec} subtypes.
 * <p>
 * Register handlers for specific frame spec derives, then resolve with a terminal method from {@link Match}.
 *
 * @param <R> the result type produced by the match
 */
public interface FrameSpecMatch<R> extends Match<FrameSpec, R> {
    /**
     * Creates a new matcher for the given {@link FrameSpec}.
     *
     * @param fs  the frame spec to match on
     * @param <R> the result type
     * @return a new {@code FrameSpecMatch} for {@code fs}
     */
    static <R> FrameSpecMatch<R> match(FrameSpec fs) {
        return new FrameSpecMatchImpl<>(fs);
    }

    /**
     * Registers a handler for a {@link FrameSpec.Single}.
     *
     * @param f handler for {@code FrameSpec.Single}
     * @return {@code this} for fluent chaining
     */
    FrameSpecMatch<R> single(Function<FrameSpec.Single, R> f);

    /**
     * Registers a handler for a {@link FrameSpec.Between}.
     *
     * @param f handler for {@code FrameSpec.Between}
     * @return {@code this} for fluent chaining
     */
    FrameSpecMatch<R> between(Function<FrameSpec.Between, R> f);
}
