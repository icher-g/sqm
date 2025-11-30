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
public class ParamsMatchImpl<R> implements ParamsMatch<R> {

    private final ParamExpr expr;
    private boolean matched = false;
    private R result;

    /**
     * Initializes {@link ParamsMatchImpl}.
     *
     * @param expr an expression to match.
     */
    public ParamsMatchImpl(ParamExpr expr) {
        this.expr = expr;
    }

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
    @Override
    public ParamsMatch<R> anonymous(Function<AnonymousParamExpr, R> f) {
        if (!matched && expr instanceof AnonymousParamExpr e) {
            result = f.apply(e);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler to be applied when the subject is a {@link NamedParamExpr}.
     *
     * @param f handler for {@code NamedParamExpr}
     * @return {@code this} for fluent chaining
     */
    @Override
    public ParamsMatch<R> named(Function<NamedParamExpr, R> f) {
        if (!matched && expr instanceof NamedParamExpr e) {
            result = f.apply(e);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler to be applied when the subject is a {@link OrdinalParamExpr}.
     *
     * @param f handler for {@code OrdinalParamExpr}
     * @return {@code this} for fluent chaining
     */
    @Override
    public ParamsMatch<R> ordinal(Function<OrdinalParamExpr, R> f) {
        if (!matched && expr instanceof OrdinalParamExpr e) {
            result = f.apply(e);
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
    public R otherwise(Function<ParamExpr, R> f) {
        return matched ? result : f.apply(expr);
    }
}
