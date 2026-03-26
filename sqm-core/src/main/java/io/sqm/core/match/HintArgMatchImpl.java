package io.sqm.core.match;

import io.sqm.core.ExpressionHintArg;
import io.sqm.core.HintArg;
import io.sqm.core.IdentifierHintArg;
import io.sqm.core.QualifiedNameHintArg;

import java.util.function.Function;

/**
 * Default matcher implementation for {@link HintArg}.
 *
 * @param <R> result type
 */
public final class HintArgMatchImpl<R> implements HintArgMatch<R> {
    private final HintArg arg;
    private boolean matched;
    private R result;

    /**
     * Creates a matcher.
     *
     * @param arg hint argument
     */
    public HintArgMatchImpl(HintArg arg) {
        this.arg = arg;
    }

    @Override
    public HintArgMatch<R> identifier(Function<IdentifierHintArg, R> function) {
        if (!matched && arg instanceof IdentifierHintArg identifierHintArg) {
            result = function.apply(identifierHintArg);
            matched = true;
        }
        return this;
    }

    @Override
    public HintArgMatch<R> qualifiedName(Function<QualifiedNameHintArg, R> function) {
        if (!matched && arg instanceof QualifiedNameHintArg qualifiedNameHintArg) {
            result = function.apply(qualifiedNameHintArg);
            matched = true;
        }
        return this;
    }

    @Override
    public HintArgMatch<R> expression(Function<ExpressionHintArg, R> function) {
        if (!matched && arg instanceof ExpressionHintArg expressionHintArg) {
            result = function.apply(expressionHintArg);
            matched = true;
        }
        return this;
    }

    @Override
    public R otherwise(Function<HintArg, R> function) {
        return matched ? result : function.apply(arg);
    }
}