package io.sqm.core.match;

import io.sqm.core.*;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link Predicate} subtypes.
 * <p>
 * Register handlers for concrete predicate kinds (expr.g., {@code BETWEEN}, {@code IN}),
 * then resolve with a terminal method from {@link Match}.
 *
 * @param <R> the result type produced by the match
 */
public interface PredicateMatch<R> extends Match<Predicate, R> {

    /**
     * Creates a new matcher for the given {@link Predicate}.
     *
     * @param p   the predicate to match on
     * @param <R> the result type
     * @return a new {@code PredicateMatch} for {@code p}
     */
    static <R> PredicateMatch<R> match(Predicate p) {
        return new PredicateMatchImpl<>(p);
    }

    /**
     * Registers a handler for {@link AnyAllPredicate}.
     *
     * @param f handler for {@code AnyAllPredicate}
     * @return {@code this} for fluent chaining
     */
    PredicateMatch<R> anyAll(Function<AnyAllPredicate, R> f);

    /**
     * Registers a handler for {@link BetweenPredicate}.
     *
     * @param f handler for {@code BetweenPredicate}
     * @return {@code this} for fluent chaining
     */
    PredicateMatch<R> between(Function<BetweenPredicate, R> f);

    /**
     * Registers a handler for {@link ComparisonPredicate}.
     *
     * @param f handler for {@code ComparisonPredicate}
     * @return {@code this} for fluent chaining
     */
    PredicateMatch<R> comparison(Function<ComparisonPredicate, R> f);

    /**
     * Registers a handler for {@link ExistsPredicate}.
     *
     * @param f handler for {@code ExistsPredicate}
     * @return {@code this} for fluent chaining
     */
    PredicateMatch<R> exists(Function<ExistsPredicate, R> f);

    /**
     * Registers a handler for {@link InPredicate}.
     *
     * @param f handler for {@code InPredicate}
     * @return {@code this} for fluent chaining
     */
    PredicateMatch<R> in(Function<InPredicate, R> f);

    /**
     * Registers a handler for {@link IsNullPredicate}.
     *
     * @param f handler for {@code IsNullPredicate}
     * @return {@code this} for fluent chaining
     */
    PredicateMatch<R> isNull(Function<IsNullPredicate, R> f);

    /**
     * Registers a handler for {@link LikePredicate}.
     *
     * @param f handler for {@code LikePredicate}
     * @return {@code this} for fluent chaining
     */
    PredicateMatch<R> like(Function<LikePredicate, R> f);

    /**
     * Registers a handler for {@link NotPredicate}.
     *
     * @param f handler for {@code NotPredicate}
     * @return {@code this} for fluent chaining
     */
    PredicateMatch<R> not(Function<NotPredicate, R> f);

    /**
     * Registers a handler for {@link UnaryPredicate}.
     *
     * @param f handler for {@code UnaryPredicate}
     * @return {@code this} for fluent chaining
     */
    PredicateMatch<R> unary(Function<UnaryPredicate, R> f);

    /**
     * Registers a handler for {@link AndPredicate}.
     *
     * @param f handler for {@code AndPredicate}
     * @return {@code this} for fluent chaining
     */
    PredicateMatch<R> and(Function<AndPredicate, R> f);

    /**
     * Registers a handler for {@link OrPredicate}.
     *
     * @param f handler for {@code OrPredicate}
     * @return {@code this} for fluent chaining
     */
    PredicateMatch<R> or(Function<OrPredicate, R> f);

    /**
     * Matches a {@link RegexPredicate}.
     *
     * <p>This matcher is invoked when the inspected predicate represents
     * a regular expression matching predicate.</p>
     *
     * @param f a mapping function applied to the matched {@link RegexPredicate}
     * @return a {@link PredicateMatch} representing this match branch
     */
    PredicateMatch<R> regex(Function<RegexPredicate, R> f);
}


