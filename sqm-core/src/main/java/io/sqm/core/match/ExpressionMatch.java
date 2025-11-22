package io.sqm.core.match;

import io.sqm.core.*;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link Expression} subtypes.
 * <p>
 * Register one or more subtype handlers (expr.g., {@link #column(Function)} or {@link #func(Function)}),
 * then finish with a terminal operation (expr.g., {@link #otherwise(Function)}).
 *
 * @param <R> the result type produced by the match
 */
public interface ExpressionMatch<R> extends Match<Expression, R> {

    /**
     * Creates a new matcher for the given {@link Expression}.
     *
     * @param e   the expression to match on (maybe any concrete {@code Expression} subtype)
     * @param <R> the result type produced by the match
     * @return a new {@code ExpressionMatch} for {@code e}
     */
    static <R> ExpressionMatch<R> match(Expression e) {
        return new ExpressionMatchImpl<>(e);
    }

    /**
     * Registers a handler to be applied when the subject is a {@link CaseExpr}.
     *
     * @param f handler for {@code CaseExpr}
     * @return {@code this} for fluent chaining
     */
    ExpressionMatch<R> kase(Function<CaseExpr, R> f);

    /**
     * Registers a handler to be applied when the subject is a {@link ColumnExpr}.
     *
     * @param f handler for {@code ColumnExpr}
     * @return {@code this} for fluent chaining
     */
    ExpressionMatch<R> column(Function<ColumnExpr, R> f);

    /**
     * Registers a handler to be applied when the subject is a {@link FunctionExpr}.
     *
     * @param f handler for {@code FunctionExpr}
     * @return {@code this} for fluent chaining
     */
    ExpressionMatch<R> func(Function<FunctionExpr, R> f);

    /**
     * Registers a handler to be applied when the subject is a {@link FunctionExpr.Arg}.
     *
     * @param f handler for {@code FunctionExpr.Arg}
     * @return {@code this} for fluent chaining
     */
    ExpressionMatch<R> funcArg(Function<FunctionExpr.Arg, R> f);

    /**
     * Registers a handler to be applied when the subject is a {@link AnonymousParamExpr}.
     *
     * @param f handler for {@code AnonymousParamExpr}
     * @return {@code this} for fluent chaining
     */
    ExpressionMatch<R> paramAnonymous(Function<AnonymousParamExpr, R> f);

    /**
     * Registers a handler to be applied when the subject is a {@link NamedParamExpr}.
     *
     * @param f handler for {@code NamedParamExpr}
     * @return {@code this} for fluent chaining
     */
    ExpressionMatch<R> paramNamed(Function<NamedParamExpr, R> f);

    /**
     * Registers a handler to be applied when the subject is a {@link OrdinalParamExpr}.
     *
     * @param f handler for {@code OrdinalParamExpr}
     * @return {@code this} for fluent chaining
     */
    ExpressionMatch<R> paramOrdinal(Function<OrdinalParamExpr, R> f);

    /**
     * Registers a handler to be applied when the subject is a {@link LiteralExpr}.
     *
     * @param f handler for {@code LiteralExpr}
     * @return {@code this} for fluent chaining
     */
    ExpressionMatch<R> literal(Function<LiteralExpr, R> f);

    /**
     * Registers a handler to be applied when the subject is a {@link ValueSet}.
     *
     * @param f handler for {@code ValueSet}
     * @return {@code this} for fluent chaining
     */
    ExpressionMatch<R> valueSet(Function<ValueSet, R> f);

    /**
     * Registers a handler to be applied when the subject is a {@link Predicate}.
     *
     * @param f handler for {@code Predicate}
     * @return {@code this} for fluent chaining
     */
    ExpressionMatch<R> predicate(Function<Predicate, R> f);
}


