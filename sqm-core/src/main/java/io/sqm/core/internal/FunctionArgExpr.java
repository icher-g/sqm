package io.sqm.core.internal;


import io.sqm.core.Expression;
import io.sqm.core.FunctionExpr;

/**
 * Implements a function argument that represents any expression.
 *
 * @param expr an expression.
 */
public record FunctionArgExpr(Expression expr) implements FunctionExpr.Arg.ExprArg {
}
