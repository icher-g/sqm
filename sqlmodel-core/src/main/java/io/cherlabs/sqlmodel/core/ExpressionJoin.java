package io.cherlabs.sqlmodel.core;

import io.cherlabs.sqlmodel.core.traits.HasExpr;

/**
 * Represents a join expr. Should be used only if the expr cannot be represented by any other join implementations.
 *
 * @param expr an expr.
 */
public record ExpressionJoin(String expr) implements Join, HasExpr {
}
