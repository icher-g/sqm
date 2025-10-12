package io.cherlabs.sqm.core;

import io.cherlabs.sqm.core.traits.HasAlias;
import io.cherlabs.sqm.core.traits.HasExpr;

/**
 * Represents an expr as a column. This class should be used only if there is no other classes that support the expr.
 *
 * @param expr  an expr
 * @param alias an alias
 */
public record ExpressionColumn(String expr, String alias) implements Column, HasExpr, HasAlias {
    /**
     * Adds an alias to the column.
     *
     * @param alias an alias.
     * @return A new instance of the column with the alias. All other fields are preserved.
     */
    public ExpressionColumn as(String alias) {
        return new ExpressionColumn(expr, alias);
    }
}
