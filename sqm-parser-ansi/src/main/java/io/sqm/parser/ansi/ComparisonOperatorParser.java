package io.sqm.parser.ansi;

import io.sqm.core.ComparisonOperator;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;

import static io.sqm.parser.core.OperatorTokens.*;

public class ComparisonOperatorParser {

    public ComparisonOperator parse(Cursor cur, ParseContext ctx) {
        if (cur.consumeIf(t -> isEq(t))) {
            return ComparisonOperator.EQ;
        }
        if (cur.consumeIf(t -> isNeqAngle(t)) || cur.consumeIf(t -> isNeqBang(t))) {
            return ComparisonOperator.NE;
        }
        if (cur.consumeIf(t -> isGt(t))) {
            return ComparisonOperator.GT;
        }
        if (cur.consumeIf(t -> isGte(t))) {
            return ComparisonOperator.GTE;
        }
        if (cur.consumeIf(t -> isLt(t))) {
            return ComparisonOperator.LT;
        }
        if (cur.consumeIf(t -> isLte(t))) {
            return ComparisonOperator.LTE;
        }
        throw new UnsupportedOperationException("The specified comparison operator is not supported: " + cur.peek().lexeme());
    }
}
