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
public class ExpressionMatchImpl<R> implements ExpressionMatch<R> {

    private final Expression expr;
    private boolean matched = false;
    private R result;

    /**
     * Initializes {@link ExpressionMatchImpl}.
     *
     * @param expr an expression to match.
     */
    public ExpressionMatchImpl(Expression expr) {
        this.expr = expr;
    }

    /**
     * Registers a handler to be applied when the subject is a {@link CaseExpr}.
     *
     * @param f handler for {@code CaseExpr}
     * @return {@code this} for fluent chaining
     */
    @Override
    public ExpressionMatch<R> kase(Function<CaseExpr, R> f) {
        if (!matched && expr instanceof CaseExpr caseExpr) {
            result = f.apply(caseExpr);
            matched = true;
        }
        return this;
    }

    /**
     * Matches a {@link CastExpr} expression.
     *
     * <p>This matcher is invoked when the inspected expression represents
     * an explicit type cast.</p>
     *
     * @param f a mapping function applied to the matched {@link CastExpr}
     * @return an {@link ExpressionMatch} representing this match branch
     */
    @Override
    public ExpressionMatch<R> cast(Function<CastExpr, R> f) {
        if (!matched && expr instanceof CastExpr castExpr) {
            result = f.apply(castExpr);
            matched = true;
        }
        return this;
    }

    /**
     * Matches an {@link ArrayExpr} expression.
     *
     * <p>This matcher is invoked when the inspected expression represents
     * an array constructor or array-valued expression.</p>
     *
     * @param f a mapping function applied to the matched {@link ArrayExpr}
     * @return an {@link ExpressionMatch} representing this match branch
     */
    @Override
    public ExpressionMatch<R> array(Function<ArrayExpr, R> f) {
        if (!matched && expr instanceof ArrayExpr arrayExpr) {
            result = f.apply(arrayExpr);
            matched = true;
        }
        return this;
    }

    /**
     * Matches an {@link ArraySubscriptExpr} expression.
     *
     * <p>This matcher is invoked when the inspected expression represents
     * an array constructor or array-valued expression.</p>
     *
     * @param f a mapping function applied to the matched {@link ArraySubscriptExpr}
     * @return an {@link ExpressionMatch} representing this match branch
     */
    @Override
    public ExpressionMatch<R> arraySubscript(Function<ArraySubscriptExpr, R> f) {
        if (!matched && expr instanceof ArraySubscriptExpr arrayExpr) {
            result = f.apply(arrayExpr);
            matched = true;
        }
        return this;
    }

    /**
     * Matches an {@link ArraySliceExpr} expression.
     *
     * <p>This matcher is invoked when the inspected expression represents
     * an array constructor or array-valued expression.</p>
     *
     * @param f a mapping function applied to the matched {@link ArraySliceExpr}
     * @return an {@link ExpressionMatch} representing this match branch
     */
    @Override
    public ExpressionMatch<R> arraySlice(Function<ArraySliceExpr, R> f) {
        if (!matched && expr instanceof ArraySliceExpr arrayExpr) {
            result = f.apply(arrayExpr);
            matched = true;
        }
        return this;
    }

    /**
     * Matches a {@link BinaryOperatorExpr} expression.
     *
     * <p>This matcher is invoked when the inspected expression represents
     * a binary operator applied to two operand expressions.</p>
     *
     * @param f a mapping function applied to the matched {@link BinaryOperatorExpr}
     * @return an {@link ExpressionMatch} representing this match branch
     */
    @Override
    public ExpressionMatch<R> binaryOperator(Function<BinaryOperatorExpr, R> f) {
        if (!matched && expr instanceof BinaryOperatorExpr binaryOperatorExpr) {
            result = f.apply(binaryOperatorExpr);
            matched = true;
        }
        return this;
    }

    /**
     * Matches a {@link UnaryOperatorExpr} expression.
     *
     * <p>This matcher is invoked when the inspected expression represents
     * a unary operator applied to a single operand expression.</p>
     *
     * @param f a mapping function applied to the matched {@link UnaryOperatorExpr}
     * @return an {@link ExpressionMatch} representing this match branch
     */
    @Override
    public ExpressionMatch<R> unaryOperator(Function<UnaryOperatorExpr, R> f) {
        if (!matched && expr instanceof UnaryOperatorExpr unaryOperatorExpr) {
            result = f.apply(unaryOperatorExpr);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler to be applied when the subject is a {@link ColumnExpr}.
     *
     * @param f handler for {@code ColumnExpr}
     * @return {@code this} for fluent chaining
     */
    @Override
    public ExpressionMatch<R> column(Function<ColumnExpr, R> f) {
        if (!matched && expr instanceof ColumnExpr columnExpr) {
            result = f.apply(columnExpr);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler to be applied when the subject is a {@link FunctionExpr}.
     *
     * @param f handler for {@code FunctionExpr}
     * @return {@code this} for fluent chaining
     */
    @Override
    public ExpressionMatch<R> func(Function<FunctionExpr, R> f) {
        if (!matched && expr instanceof FunctionExpr functionExpr) {
            result = f.apply(functionExpr);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler to be applied when the subject is a {@link FunctionExpr.Arg}.
     *
     * @param f handler for {@code FunctionExpr.Arg}
     * @return {@code this} for fluent chaining
     */
    @Override
    public ExpressionMatch<R> funcArg(Function<FunctionExpr.Arg, R> f) {
        if (!matched && expr instanceof FunctionExpr.Arg arg) {
            result = f.apply(arg);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler to be applied when the subject is a {@link ParamExpr}.
     *
     * @param f handler for {@code ParamExpr}
     * @return {@code this} for fluent chaining
     */
    @Override
    public ExpressionMatch<R> param(Function<ParamExpr, R> f) {
        if (!matched && expr instanceof ParamExpr e) {
            result = f.apply(e);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler to be applied when the subject is a {@link LiteralExpr}.
     *
     * @param f handler for {@code LiteralExpr}
     * @return {@code this} for fluent chaining
     */
    @Override
    public ExpressionMatch<R> literal(Function<LiteralExpr, R> f) {
        if (!matched && expr instanceof LiteralExpr literalExpr) {
            result = f.apply(literalExpr);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler to be applied when the subject is a {@link ValueSet}.
     *
     * @param f handler for {@code ValueSet}
     * @return {@code this} for fluent chaining
     */
    @Override
    public ExpressionMatch<R> valueSet(Function<ValueSet, R> f) {
        if (!matched && expr instanceof ValueSet valueSet) {
            result = f.apply(valueSet);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler to be applied when the subject is a {@link Predicate}.
     *
     * @param f handler for {@code Predicate}
     * @return {@code this} for fluent chaining
     */
    @Override
    public ExpressionMatch<R> predicate(Function<Predicate, R> f) {
        if (!matched && expr instanceof Predicate predicate) {
            result = f.apply(predicate);
            matched = true;
        }
        return this;
    }

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
    @Override
    public ExpressionMatch<R> arithmetic(Function<ArithmeticExpr, R> f) {
        if (!matched && expr instanceof ArithmeticExpr arithmeticExpr) {
            result = f.apply(arithmeticExpr);
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
    public R otherwise(Function<Expression, R> f) {
        return matched ? result : f.apply(expr);
    }
}
