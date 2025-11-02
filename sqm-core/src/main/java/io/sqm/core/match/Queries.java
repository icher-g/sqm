package io.sqm.core.match;

import io.sqm.core.Query;

public final class Queries {
    private Queries() {
    }

    /**
     * Creates a new matcher for the given {@link Query}.
     *
     * @param q   the query to match on
     * @param <R> the result type
     * @return a new {@code QueryMatch} for {@code q}
     */
    public static <R> QueryMatch<R> match(Query q) {
        return QueryMatch.match(q);
    }
}
