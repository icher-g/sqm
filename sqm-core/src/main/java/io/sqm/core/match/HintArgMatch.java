package io.sqm.core.match;

import io.sqm.core.ExpressionHintArg;
import io.sqm.core.HintArg;
import io.sqm.core.IdentifierHintArg;
import io.sqm.core.QualifiedNameHintArg;

import java.util.function.Function;

/**
 * Pattern-style matcher for {@link HintArg} variants.
 *
 * @param <R> result type
 */
public interface HintArgMatch<R> extends Match<HintArg, R> {

    /**
     * Creates a matcher for the given hint argument.
     *
     * @param arg hint argument
     * @param <R> result type
     * @return matcher
     */
    static <R> HintArgMatch<R> match(HintArg arg) {
        return new HintArgMatchImpl<>(arg);
    }

    /**
     * Registers a handler for identifier arguments.
     *
     * @param function handler
     * @return this matcher
     */
    HintArgMatch<R> identifier(Function<IdentifierHintArg, R> function);

    /**
     * Registers a handler for qualified-name arguments.
     *
     * @param function handler
     * @return this matcher
     */
    HintArgMatch<R> qualifiedName(Function<QualifiedNameHintArg, R> function);

    /**
     * Registers a handler for expression arguments.
     *
     * @param function handler
     * @return this matcher
     */
    HintArgMatch<R> expression(Function<ExpressionHintArg, R> function);
}