package io.sqm.core.match;

import io.sqm.core.SelectItem;

public class SelectItems {
    private SelectItems() {
    }

    /**
     * Creates a new matcher for the given {@link SelectItem}.
     *
     * @param i   the select item to match on
     * @param <R> the result type
     * @return a new {@code SelectItemMatch} for {@code i}
     */
    static <R> SelectItemMatch<R> match(SelectItem i) {
        return SelectItemMatch.match(i);
    }
}
