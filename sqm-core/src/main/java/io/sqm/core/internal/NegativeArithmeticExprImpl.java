package io.sqm.core.internal;

import io.sqm.core.Expression;
import io.sqm.core.NegativeArithmeticExpr;

/**
 * A unary arithmetic expression that represents numeric negation.
 *
 * <p>This corresponds to the SQL syntax {@code -expr}. It changes the sign of
 * the underlying expression but does not introduce additional semantics.</p>
 *
 * <p>Unary plus is intentionally not modeled, since {@code +expr} has no
 * semantic effect and can be safely ignored by the parser.</p>
 *
 * @param expr the expression to negate, must not be {@code null}
 */
public record NegativeArithmeticExprImpl(Expression expr) implements NegativeArithmeticExpr {
}
