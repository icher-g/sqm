package io.sqm.core.match;

import io.sqm.core.*;

import java.util.function.Function;

/**
 * Default matcher implementation for {@link Predicate}.
 *
 * @param <R> result type
 */
public class PredicateMatchImpl<R> implements PredicateMatch<R> {

    private final Predicate predicate;
    private boolean matched = false;
    private R result;

    /**
     * Initializes a match builder for {@link Predicate}.
     *
     * @param predicate predicate to match
     */
    public PredicateMatchImpl(Predicate predicate) {
        this.predicate = predicate;
    }

    /**
     * Registers a handler for {@link AnyAllPredicate}.
     *
     * @param f handler for {@code AnyAllPredicate}
     * @return {@code this} for fluent chaining
     */
    @Override
    public PredicateMatch<R> anyAll(Function<AnyAllPredicate, R> f) {
        if (!matched && predicate instanceof AnyAllPredicate p) {
            result = f.apply(p);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for {@link BetweenPredicate}.
     *
     * @param f handler for {@code BetweenPredicate}
     * @return {@code this} for fluent chaining
     */
    @Override
    public PredicateMatch<R> between(Function<BetweenPredicate, R> f) {
        if (!matched && predicate instanceof BetweenPredicate p) {
            result = f.apply(p);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for {@link ComparisonPredicate}.
     *
     * @param f handler for {@code ComparisonPredicate}
     * @return {@code this} for fluent chaining
     */
    @Override
    public PredicateMatch<R> comparison(Function<ComparisonPredicate, R> f) {
        if (!matched && predicate instanceof ComparisonPredicate p) {
            result = f.apply(p);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for {@link ExistsPredicate}.
     *
     * @param f handler for {@code ExistsPredicate}
     * @return {@code this} for fluent chaining
     */
    @Override
    public PredicateMatch<R> exists(Function<ExistsPredicate, R> f) {
        if (!matched && predicate instanceof ExistsPredicate p) {
            result = f.apply(p);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for {@link InPredicate}.
     *
     * @param f handler for {@code InPredicate}
     * @return {@code this} for fluent chaining
     */
    @Override
    public PredicateMatch<R> in(Function<InPredicate, R> f) {
        if (!matched && predicate instanceof InPredicate p) {
            result = f.apply(p);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for {@link IsNullPredicate}.
     *
     * @param f handler for {@code IsNullPredicate}
     * @return {@code this} for fluent chaining
     */
    @Override
    public PredicateMatch<R> isNull(Function<IsNullPredicate, R> f) {
        if (!matched && predicate instanceof IsNullPredicate p) {
            result = f.apply(p);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for {@link LikePredicate}.
     *
     * @param f handler for {@code LikePredicate}
     * @return {@code this} for fluent chaining
     */
    @Override
    public PredicateMatch<R> like(Function<LikePredicate, R> f) {
        if (!matched && predicate instanceof LikePredicate p) {
            result = f.apply(p);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for {@link NotPredicate}.
     *
     * @param f handler for {@code NotPredicate}
     * @return {@code this} for fluent chaining
     */
    @Override
    public PredicateMatch<R> not(Function<NotPredicate, R> f) {
        if (!matched && predicate instanceof NotPredicate p) {
            result = f.apply(p);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for {@link UnaryPredicate}.
     *
     * @param f handler for {@code UnaryPredicate}
     * @return {@code this} for fluent chaining
     */
    @Override
    public PredicateMatch<R> unary(Function<UnaryPredicate, R> f) {
        if (!matched && predicate instanceof UnaryPredicate p) {
            result = f.apply(p);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for {@link AndPredicate}.
     *
     * @param f handler for {@code AndPredicate}
     * @return {@code this} for fluent chaining
     */
    @Override
    public PredicateMatch<R> and(Function<AndPredicate, R> f) {
        if (!matched && predicate instanceof AndPredicate p) {
            result = f.apply(p);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for {@link OrPredicate}.
     *
     * @param f handler for {@code OrPredicate}
     * @return {@code this} for fluent chaining
     */
    @Override
    public PredicateMatch<R> or(Function<OrPredicate, R> f) {
        if (!matched && predicate instanceof OrPredicate p) {
            result = f.apply(p);
            matched = true;
        }
        return this;
    }

    /**
     * Matches a {@link RegexPredicate}.
     *
     * <p>This matcher is invoked when the inspected predicate represents
     * a regular expression matching predicate.</p>
     *
     * @param f a mapping function applied to the matched {@link RegexPredicate}
     * @return a {@link PredicateMatch} representing this match branch
     */
    @Override
    public PredicateMatch<R> regex(Function<RegexPredicate, R> f) {
        if (!matched && predicate instanceof RegexPredicate p) {
            result = f.apply(p);
            matched = true;
        }
        return this;
    }

    /**
     * Matches an IS DISTINCT FROM / IS NOT DISTINCT FROM predicate.
     *
     * @param f a mapping function for the predicate.
     * @return this matcher instance.
     */
    @Override
    public PredicateMatch<R> isDistinctFrom(Function<IsDistinctFromPredicate, R> f) {
        if (!matched && predicate instanceof IsDistinctFromPredicate p) {
            result = f.apply(p);
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
    public R otherwise(Function<Predicate, R> f) {
        return matched ? result : f.apply(predicate);
    }
}
