package io.sqm.core.match;

import io.sqm.core.*;
import io.sqm.core.walk.NodeVisitor;

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
     * Matches a {@link CastExpr} expression.
     *
     * <p>This matcher is invoked when the inspected expression represents
     * an explicit type cast.</p>
     *
     * @param f a mapping function applied to the matched {@link CastExpr}
     * @return an {@link ExpressionMatch} representing this match branch
     */
    ExpressionMatch<R> cast(Function<CastExpr, R> f);

    /**
     * Matches an {@link ArrayExpr} expression.
     *
     * <p>This matcher is invoked when the inspected expression represents
     * an array constructor or array-valued expression.</p>
     *
     * @param f a mapping function applied to the matched {@link ArrayExpr}
     * @return an {@link ExpressionMatch} representing this match branch
     */
    ExpressionMatch<R> array(Function<ArrayExpr, R> f);

    /**
     * Matches an {@link ArraySubscriptExpr} expression.
     *
     * <p>This matcher is invoked when the inspected expression represents
     * an array constructor or array-valued expression.</p>
     *
     * @param f a mapping function applied to the matched {@link ArraySubscriptExpr}
     * @return an {@link ExpressionMatch} representing this match branch
     */
    ExpressionMatch<R> arraySubscript(Function<ArraySubscriptExpr, R> f);

    /**
     * Matches an {@link ArraySliceExpr} expression.
     *
     * <p>This matcher is invoked when the inspected expression represents
     * an array constructor or array-valued expression.</p>
     *
     * @param f a mapping function applied to the matched {@link ArraySliceExpr}
     * @return an {@link ExpressionMatch} representing this match branch
     */
    ExpressionMatch<R> arraySlice(Function<ArraySliceExpr, R> f);

    /**
     * Matches a {@link BinaryOperatorExpr} expression.
     *
     * <p>This matcher is invoked when the inspected expression represents
     * a binary operator applied to two operand expressions.</p>
     *
     * @param f a mapping function applied to the matched {@link BinaryOperatorExpr}
     * @return an {@link ExpressionMatch} representing this match branch
     */
    ExpressionMatch<R> binaryOperator(Function<BinaryOperatorExpr, R> f);

    /**
     * Matches a {@link UnaryOperatorExpr} expression.
     *
     * <p>This matcher is invoked when the inspected expression represents
     * a unary operator applied to a single operand expression.</p>
     *
     * @param f a mapping function applied to the matched {@link UnaryOperatorExpr}
     * @return an {@link ExpressionMatch} representing this match branch
     */
    ExpressionMatch<R> unaryOperator(Function<UnaryOperatorExpr, R> f);

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
     * Registers a handler to be applied when the subject is a {@link ParamExpr}.
     *
     * @param f handler for {@code ParamExpr}
     * @return {@code this} for fluent chaining
     */
    ExpressionMatch<R> param(Function<ParamExpr, R> f);

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

    /**
     * Matches any arithmetic expression, including binary operations
     * ({@code +}, {@code -}, {@code *}, {@code /}, {@code %}) and unary
     * negation ({@code -expr}).
     *
     * <p>If the expression being matched is an instance of
     * {@link ArithmeticExpr} or any of its subtypes, the supplied function
     * is invoked with that expression and its result becomes the return value
     * of the match operation.</p>
     *
     * <p>This method does not recursively inspect the operands of an
     * arithmetic expression; it only performs type-based dispatch. For
     * structural traversal, use a {@link NodeVisitor} or dedicated transformer.</p>
     *
     * @param f a function applied when the matched expression is an
     *          {@link ArithmeticExpr}; must not be {@code null}
     * @return this matcher instance for method chaining
     */
    ExpressionMatch<R> arithmetic(Function<ArithmeticExpr, R> f);
}


