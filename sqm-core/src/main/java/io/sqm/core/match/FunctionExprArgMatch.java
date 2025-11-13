package io.sqm.core.match;

import io.sqm.core.FunctionExpr;

import java.util.function.Function;

/**
 * A matcher for function arguments.
 *
 * @param <R> a return type of the matcher.
 */
public interface FunctionExprArgMatch<R> extends Match<FunctionExpr.Arg, R> {
    /**
     * Creates a new matcher for the given {@link FunctionExpr.Arg}.
     *
     * @param a   the function argument to match on (maybe any concrete {@code FunctionExpr.Arg} subtype)
     * @param <R> the result type produced by the match
     * @return a new {@code FunctionExprArgMatch} for {@code a}
     */
    static <R> FunctionExprArgMatch<R> match(FunctionExpr.Arg a) {
        return new FunctionExprArgMatchImpl<>(a);
    }

    /**
     * Registers a handler to be applied when the subject is a {@link FunctionExpr.Arg.ExprArg}.
     *
     * @param f handler for {@code FunctionExpr.Arg.ExprArg}
     * @return {@code this} for fluent chaining
     */
    FunctionExprArgMatch<R> exprArg(Function<FunctionExpr.Arg.ExprArg, R> f);

    /**
     * Registers a handler to be applied when the subject is a {@link FunctionExpr.Arg.StarArg}.
     *
     * @param f handler for {@code FunctionExpr.Arg.StarArg}
     * @return {@code this} for fluent chaining
     */
    FunctionExprArgMatch<R> starArg(Function<FunctionExpr.Arg.StarArg, R> f);
}
