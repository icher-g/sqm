package io.sqm.core.internal;

import io.sqm.core.Expression;
import io.sqm.core.LiteralExpr;

import java.math.BigDecimal;

/**
 * Structural checks shared by MODEL value generators.
 */
public final class ModelChecks {
    private ModelChecks() {
    }

    /**
     * Rejects a numeric literal outside the positive range.
     * Expressions whose values require evaluation are left to dialect validation.
     *
     * @param expression candidate value
     * @param integral   whether the value must be an integer
     * @param label      diagnostic label
     */
    public static void positiveLiteral(Expression expression, boolean integral, String label) {
        if (expression instanceof LiteralExpr literal && literal.value() instanceof Number number) {
            BigDecimal value;
            try {
                value = new BigDecimal(number.toString());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(label + " must be finite", exception);
            }
            if (value.signum() <= 0 || (integral && value.stripTrailingZeros().scale() > 0)) {
                throw new IllegalArgumentException(label + " must be positive" + (integral ? " and integral" : ""));
            }
        }
    }
}