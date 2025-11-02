package io.sqm.core.match;

import io.sqm.core.*;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link Expression} subtypes.
 * <p>
 * Register one or more subtype handlers (e.g., {@link #column(Function)} or {@link #func(Function)}),
 * then finish with a terminal operation (e.g., {@link #otherwise(Function)}).
 *
 * @param <R> the result type produced by the match
 */
public interface ExpressionMatch<R> extends Match<Expression, R> {

    /**
     * Creates a new matcher for the given {@link Expression}.
     *
     * @param e   the expression to match on (may be any concrete {@code Expression} subtype)
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
     * Registers a handler to be applied when the subject is a {@link LiteralExpr}.
     *
     * @param f handler for {@code LiteralExpr}
     * @return {@code this} for fluent chaining
     */
    ExpressionMatch<R> literal(Function<LiteralExpr, R> f);

    /**
     * Registers a handler to be applied when the subject is a {@link RowExpr}.
     *
     * @param f handler for {@code RowExpr}
     * @return {@code this} for fluent chaining
     */
    ExpressionMatch<R> row(Function<RowExpr, R> f);

    /**
     * Registers a handler to be applied when the subject is a {@link RowListExpr}.
     *
     * @param f handler for {@code RowListExpr}
     * @return {@code this} for fluent chaining
     */
    ExpressionMatch<R> rows(Function<RowListExpr, R> f);

    /**
     * Registers a handler to be applied when the subject is a {@link QueryExpr}.
     *
     * @param f handler for {@code QueryExpr}
     * @return {@code this} for fluent chaining
     */
    ExpressionMatch<R> query(Function<QueryExpr, R> f);
}


