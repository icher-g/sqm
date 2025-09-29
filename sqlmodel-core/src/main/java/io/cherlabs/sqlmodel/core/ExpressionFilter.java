package io.cherlabs.sqlmodel.core;

import io.cherlabs.sqlmodel.core.traits.HasExpr;

/**
 * Represents a filter expr. Should only be used if the expr can not be represented by any other filter classes.
 *
 * @param expr an expr
 */
public record ExpressionFilter(String expr) implements Filter, HasExpr {
}
