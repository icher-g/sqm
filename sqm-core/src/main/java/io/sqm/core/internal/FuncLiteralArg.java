package io.sqm.core.internal;

import io.sqm.core.FunctionExpr;
import io.sqm.core.Node;

/**
 * Implements a function literal argument.
 *
 * @param value a value.
 */
public record FuncLiteralArg(Object value) implements FunctionExpr.Arg.Literal {

    public FuncLiteralArg {
        if (value instanceof Node) {
            throw new IllegalArgumentException("Literal value should not be: " + value.getClass().getSimpleName());
        }
    }
}
