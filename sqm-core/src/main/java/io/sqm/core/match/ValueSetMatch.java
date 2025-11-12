package io.sqm.core.match;

import io.sqm.core.*;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link ValueSet} subtypes.
 * <p>
 * Register one or more subtype handlers (expr.g., {@link #row(Function)} or {@link #rows(Function)}),
 * then finish with a terminal operation (expr.g., {@link #otherwise(Function)}).
 *
 * @param <R> the result type produced by the match
 */
public interface ValueSetMatch<R> extends Match<ValueSet, R> {

    /**
     * Creates a new matcher for the given {@link Expression}.
     *
     * @param v   the value set to match on (maybe any concrete {@code ValueSet} subtype)
     * @param <R> the result type produced by the match
     * @return a new {@code ExpressionMatch} for {@code expr}
     */
    static <R> ValueSetMatch<R> match(ValueSet v) {
        return new ValueSetMatchImpl<>(v);
    }

    /**
     * Registers a handler to be applied when the subject is a {@link RowExpr}.
     *
     * @param f handler for {@code RowExpr}
     * @return {@code this} for fluent chaining
     */
    ValueSetMatch<R> row(Function<RowExpr, R> f);

    /**
     * Registers a handler to be applied when the subject is a {@link RowListExpr}.
     *
     * @param f handler for {@code RowListExpr}
     * @return {@code this} for fluent chaining
     */
    ValueSetMatch<R> rows(Function<RowListExpr, R> f);

    /**
     * Registers a handler to be applied when the subject is a {@link QueryExpr}.
     *
     * @param f handler for {@code QueryExpr}
     * @return {@code this} for fluent chaining
     */
    ValueSetMatch<R> query(Function<QueryExpr, R> f);
}
