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
