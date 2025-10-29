package io.sqm.core.internal;

import io.sqm.core.FunctionExpr;

/**
 * Implements a func argument representing another function call.
 *
 * @param call a function call.
 */
public record FuncCallArg(FunctionExpr call) implements FunctionExpr.Arg.Function {
}
