package io.sqm.core.internal;

import io.sqm.core.AnonymousParamExpr;

/**
 * An anonymous positional parameter, typically represented as {@code ?} in SQL.
 * <p>
 * Anonymous parameters do not carry an explicit index in the SQL text.
 * Instead, a logical position is assigned by the parser based on the
 * order in which parameters appear in the query.
 */
public record AnonymousParamExprImpl() implements AnonymousParamExpr {
}
