package io.sqm.parser.ansi;

import io.sqm.core.ComparisonOperator;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;

import static io.sqm.parser.core.OperatorTokens.*;

/**
 * Parses comparison operator tokens into {@link ComparisonOperator} values.
 */
public class ComparisonOperatorParser {
    /**
     * Creates a comparison operator parser.
     */
    public ComparisonOperatorParser() {
    }

    /**
     * Parses the current token sequence as a comparison operator.
     *
     * @param cur token cursor at the operator token
     * @param ctx parsing context
     * @return parsed comparison operator
     */
    @SuppressWarnings("unused")
    public ComparisonOperator parse(Cursor cur, ParseContext ctx) {
        ComparisonOperator operator;
        if (cur.consumeIf(t -> isEq(t))) {
            operator = ComparisonOperator.EQ;
        }
        else if (cur.consumeIf(t -> isNullSafeEq(t))) {
            operator = ComparisonOperator.NULL_SAFE_EQ;
        }
        else if (cur.consumeIf(t -> isNeqAngle(t)) || cur.consumeIf(t -> isNeqBang(t))) {
            operator = ComparisonOperator.NE;
        }
        else if (cur.consumeIf(t -> isGt(t))) {
            operator = ComparisonOperator.GT;
        }
        else if (cur.consumeIf(t -> isGte(t))) {
            operator = ComparisonOperator.GTE;
        }
        else if (cur.consumeIf(t -> isLt(t))) {
            operator = ComparisonOperator.LT;
        }
        else if (cur.consumeIf(t -> isLte(t))) {
            operator = ComparisonOperator.LTE;
        }
        else {
            throw new UnsupportedOperationException("The specified comparison operator is not supported: " + cur.peek().lexeme());
        }

        operator.assertSupported(ctx.capabilities());
        return operator;
    }
}
