package io.sqm.core.match;

import io.sqm.core.Join;

public class Joins {
    private Joins() {
    }

    /**
     * Creates a new matcher for the given {@link Join}.
     *
     * @param j   the join to match on
     * @param <R> the result type
     * @return a new {@code JoinMatch} for {@code j}
     */
    static <R> JoinMatch<R> match(Join j) {
        return JoinMatch.match(j);
    }
}
