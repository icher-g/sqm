package io.sqm.parser.ansi;

import io.sqm.core.ComparisonOperator;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;

public class ComparisonOperatorParser {

    public ComparisonOperator parse(Cursor cur, ParseContext ctx) {
        if (cur.consumeIf(TokenType.EQ)) {
            return ComparisonOperator.EQ;
        }
        if (cur.consumeIf(TokenType.NEQ1) || cur.consumeIf(TokenType.NEQ2)) {
            return ComparisonOperator.NE;
        }
        if (cur.consumeIf(TokenType.GT)) {
            return ComparisonOperator.GT;
        }
        if (cur.consumeIf(TokenType.GTE)) {
            return ComparisonOperator.GTE;
        }
        if (cur.consumeIf(TokenType.LT)) {
            return ComparisonOperator.LT;
        }
        if (cur.consumeIf(TokenType.LTE)) {
            return ComparisonOperator.LTE;
        }
        throw new UnsupportedOperationException("The specified comparison operator is not supported: " + cur.peek().lexeme());
    }
}
