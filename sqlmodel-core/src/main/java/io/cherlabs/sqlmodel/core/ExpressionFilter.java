package io.cherlabs.sqlmodel.core;

import io.cherlabs.sqlmodel.core.traits.HasExpr;

public record ExpressionFilter(String expression) implements Filter, HasExpr {
}
