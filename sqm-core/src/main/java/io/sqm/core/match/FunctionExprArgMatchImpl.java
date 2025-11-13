package io.sqm.core.match;

import io.sqm.core.FunctionExpr;

import java.util.function.Function;

/**
 * A matcher for function arguments.
 *
 * @param <R> a return type of the matcher.
 */
public class FunctionExprArgMatchImpl<R> implements FunctionExprArgMatch<R> {

    private final FunctionExpr.Arg arg;
    private boolean matched = false;
    private R result;

    /**
     * Initializes a new instance of {@link FunctionExprArgMatch}
     *
     * @param arg a function argument to match.
     */
    public FunctionExprArgMatchImpl(FunctionExpr.Arg arg) {
        this.arg = arg;
    }

    /**
     * Registers a handler to be applied when the subject is a {@link FunctionExpr.Arg.ExprArg}.
     *
     * @param f handler for {@code FunctionExpr.Arg.ExprArg}
     * @return {@code this} for fluent chaining
     */
    @Override
    public FunctionExprArgMatch<R> exprArg(Function<FunctionExpr.Arg.ExprArg, R> f) {
        if (!matched && arg instanceof FunctionExpr.Arg.ExprArg a) {
            result = f.apply(a);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler to be applied when the subject is a {@link FunctionExpr.Arg.StarArg}.
     *
     * @param f handler for {@code FunctionExpr.Arg.StarArg}
     * @return {@code this} for fluent chaining
     */
    @Override
    public FunctionExprArgMatch<R> starArg(Function<FunctionExpr.Arg.StarArg, R> f) {
        if (!matched && arg instanceof FunctionExpr.Arg.StarArg a) {
            result = f.apply(a);
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
    public R otherwise(Function<FunctionExpr.Arg, R> f) {
        return matched ? result : f.apply(arg);
    }
}
