package io.sqm.core.match;

import io.sqm.core.Predicate;

public final class Predicates {
    private Predicates() {
    }

    /**
     * Creates a new matcher for the given {@link Predicate}.
     *
     * @param p   the predicate to match on
     * @param <R> the result type
     * @return a new {@code PredicateMatch} for {@code p}
     */
    static <R> PredicateMatch<R> match(Predicate p) {
        return PredicateMatch.match(p);
    }
}
