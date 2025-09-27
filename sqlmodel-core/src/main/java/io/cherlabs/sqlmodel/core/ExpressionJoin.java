package io.cherlabs.sqlmodel.core;

import io.cherlabs.sqlmodel.core.traits.HasExpr;

public record ExpressionJoin(String expression) implements Join, HasExpr {
}
