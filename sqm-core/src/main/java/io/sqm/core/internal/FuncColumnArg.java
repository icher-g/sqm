package io.sqm.core.internal;

import io.sqm.core.ColumnExpr;
import io.sqm.core.FunctionExpr;

/**
 * Implements a function argument that represents a column.
 *
 * @param ref a reference to a column.
 */
public record FuncColumnArg(ColumnExpr ref) implements FunctionExpr.Arg.Column {
}
