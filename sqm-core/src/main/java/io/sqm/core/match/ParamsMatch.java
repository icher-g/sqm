package io.sqm.core.match;

import io.sqm.core.AnonymousParamExpr;
import io.sqm.core.NamedParamExpr;
import io.sqm.core.OrdinalParamExpr;
import io.sqm.core.ParamExpr;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link ParamExpr} subtypes.
 *
 * @param <R> the result type produced by the match
 */
public interface ParamsMatch<R> extends Match<ParamExpr, R> {
    /**
     * Creates a new matcher for the given {@link ParamExpr}.
     *
     * @param e   the expression to match on (maybe any concrete {@code ParamExpr} subtype)
     * @param <R> the result type produced by the match
     * @return a new {@code ParamsMatch} for {@code e}
     */
    static <R> ParamsMatch<R> match(ParamExpr e) {
        return new ParamsMatchImpl<>(e);
    }

    /**
     * Registers a handler to be applied when the subject is a {@link AnonymousParamExpr}.
     *
     * @param f handler for {@code AnonymousParamExpr}
     * @return {@code this} for fluent chaining
     */
    ParamsMatch<R> anonymous(Function<AnonymousParamExpr, R> f);

    /**
     * Registers a handler to be applied when the subject is a {@link NamedParamExpr}.
     *
     * @param f handler for {@code NamedParamExpr}
     * @return {@code this} for fluent chaining
     */
    ParamsMatch<R> named(Function<NamedParamExpr, R> f);

    /**
     * Registers a handler to be applied when the subject is a {@link OrdinalParamExpr}.
     *
     * @param f handler for {@code OrdinalParamExpr}
     * @return {@code this} for fluent chaining
     */
    ParamsMatch<R> ordinal(Function<OrdinalParamExpr, R> f);
}
