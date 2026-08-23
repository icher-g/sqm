package io.sqm.core.match;

import io.sqm.core.MatchPattern;

import java.util.Objects;
import java.util.function.Function;

/**
 * Default matcher implementation for {@link MatchPattern} variants.
 *
 * @param <R> match result type
 */
public final class PatternMatchImpl<R> implements PatternMatch<R> {
    private final MatchPattern pattern;
    private boolean matched;
    private R result;

    /**
     * Creates a matcher for the supplied pattern.
     *
     * @param pattern pattern to match
     */
    public PatternMatchImpl(MatchPattern pattern) {
        this.pattern = Objects.requireNonNull(pattern, "pattern");
    }

    /** {@inheritDoc} */
    @Override
    public PatternMatch<R> variable(Function<MatchPattern.Variable, R> handler) {
        if (!matched && pattern instanceof MatchPattern.Variable value) {
            result = handler.apply(value);
            matched = true;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public PatternMatch<R> sequence(Function<MatchPattern.Sequence, R> handler) {
        if (!matched && pattern instanceof MatchPattern.Sequence value) {
            result = handler.apply(value);
            matched = true;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public PatternMatch<R> alternation(Function<MatchPattern.Alternation, R> handler) {
        if (!matched && pattern instanceof MatchPattern.Alternation value) {
            result = handler.apply(value);
            matched = true;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public PatternMatch<R> permutation(Function<MatchPattern.Permutation, R> handler) {
        if (!matched && pattern instanceof MatchPattern.Permutation value) {
            result = handler.apply(value);
            matched = true;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public PatternMatch<R> anchor(Function<MatchPattern.Anchor, R> handler) {
        if (!matched && pattern instanceof MatchPattern.Anchor value) {
            result = handler.apply(value);
            matched = true;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public PatternMatch<R> empty(Function<MatchPattern.Empty, R> handler) {
        if (!matched && pattern instanceof MatchPattern.Empty value) {
            result = handler.apply(value);
            matched = true;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public PatternMatch<R> exclusion(Function<MatchPattern.Exclusion, R> handler) {
        if (!matched && pattern instanceof MatchPattern.Exclusion value) {
            result = handler.apply(value);
            matched = true;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public PatternMatch<R> quantified(Function<MatchPattern.Quantified, R> handler) {
        if (!matched && pattern instanceof MatchPattern.Quantified value) {
            result = handler.apply(value);
            matched = true;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public R otherwise(Function<MatchPattern, R> fallback) {
        return matched ? result : fallback.apply(pattern);
    }
}
