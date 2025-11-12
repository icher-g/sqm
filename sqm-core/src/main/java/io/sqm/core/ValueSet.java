package io.sqm.core;

import io.sqm.core.match.ValueSetMatch;

/**
 * Marker for value-set expressions (expr.g., IN (1,2,3), (a,b) IN ((1,2),(3,4)), IN (SELECT ...)).
 */
public sealed interface ValueSet extends Expression permits RowExpr, QueryExpr, RowListExpr {
    /**
     * Creates a new matcher for the current {@link ValueSet}.
     *
     * @param <R> the result type produced by the match
     * @return a new {@code ValueSetMatch}.
     */
    default <R> ValueSetMatch<R> matchValueSet() {
        return ValueSetMatch.match(this);
    }
}
