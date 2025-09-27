package io.cherlabs.sqlmodel.core;

import io.cherlabs.sqlmodel.core.traits.HasAlias;
import io.cherlabs.sqlmodel.core.traits.HasExpr;

public record ExpressionColumn(String expression, String alias) implements Column, HasExpr, HasAlias {
    public ExpressionColumn as(String alias) {
        return new ExpressionColumn(expression, alias);
    }
}
