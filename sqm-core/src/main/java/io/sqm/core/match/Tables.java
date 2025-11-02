package io.sqm.core.match;

import io.sqm.core.TableRef;

public class Tables {
    private Tables() {
    }

    /**
     * Creates a new matcher for the given {@link TableRef}.
     *
     * @param t   the table reference to match on
     * @param <R> the result type
     * @return a new {@code TableMatch} for {@code t}
     */
    static <R> TableMatch<R> match(TableRef t) {
        return TableMatch.match(t);
    }
}
