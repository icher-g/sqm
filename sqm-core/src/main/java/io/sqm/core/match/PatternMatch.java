package io.sqm.core.match;

import io.sqm.core.MatchPattern;

import java.util.function.Function;

/**
 * Pattern-style matcher for the {@link MatchPattern} variant family.
 *
 * @param <R> match result type
 */
public interface PatternMatch<R> extends Match<MatchPattern, R> {
    /**
     * Creates a matcher for a match pattern.
     *
     * @param pattern pattern to match
     * @param <R> match result type
     * @return pattern matcher
     */
    static <R> PatternMatch<R> match(MatchPattern pattern) {
        return new PatternMatchImpl<>(pattern);
    }

    /**
     * Registers a variable-pattern handler.
     *
     * @param handler handler to register
     * @return this matcher
     */
    PatternMatch<R> variable(Function<MatchPattern.Variable, R> handler);

    /**
     * Registers a sequence-pattern handler.
     *
     * @param handler handler to register
     * @return this matcher
     */
    PatternMatch<R> sequence(Function<MatchPattern.Sequence, R> handler);

    /**
     * Registers an alternation-pattern handler.
     *
     * @param handler handler to register
     * @return this matcher
     */
    PatternMatch<R> alternation(Function<MatchPattern.Alternation, R> handler);

    /**
     * Registers a permutation-pattern handler.
     *
     * @param handler handler to register
     * @return this matcher
     */
    PatternMatch<R> permutation(Function<MatchPattern.Permutation, R> handler);

    /**
     * Registers an anchor-pattern handler.
     *
     * @param handler handler to register
     * @return this matcher
     */
    PatternMatch<R> anchor(Function<MatchPattern.Anchor, R> handler);

    /**
     * Registers an empty-pattern handler.
     *
     * @param handler handler to register
     * @return this matcher
     */
    PatternMatch<R> empty(Function<MatchPattern.Empty, R> handler);

    /**
     * Registers an exclusion-pattern handler.
     *
     * @param handler handler to register
     * @return this matcher
     */
    PatternMatch<R> exclusion(Function<MatchPattern.Exclusion, R> handler);

    /**
     * Registers a quantified-pattern handler.
     *
     * @param handler handler to register
     * @return this matcher
     */
    PatternMatch<R> quantified(Function<MatchPattern.Quantified, R> handler);
}
