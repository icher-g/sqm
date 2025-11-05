package io.sqm.core.internal;

import io.sqm.core.FunctionExpr;

/**
 * Implements a function argument '*'.
 */
public record FuncStarArg() implements FunctionExpr.Arg.StarArg {
}
