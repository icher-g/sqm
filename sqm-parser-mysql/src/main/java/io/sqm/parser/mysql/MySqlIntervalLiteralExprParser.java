package io.sqm.parser.mysql;

import io.sqm.parser.ansi.IntervalLiteralExprParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;

/**
 * MySQL interval-literal parser supporting both quoted and unquoted numeric
 * interval forms such as {@code INTERVAL '1' DAY} and {@code INTERVAL 1 DAY}.
 */
public class MySqlIntervalLiteralExprParser extends IntervalLiteralExprParser {
    /**
     * Creates a MySQL interval-literal parser.
     */
    public MySqlIntervalLiteralExprParser() {
    }

    /**
     * Parses the interval literal body after the {@code INTERVAL} keyword.
     *
     * @param cur current token cursor
     * @return interval literal value without surrounding quotes
     */
    @Override
    protected String parseLiteralValue(Cursor cur) {
        if (cur.match(TokenType.OPERATOR) && "-".equals(cur.peek().lexeme()) && cur.match(TokenType.NUMBER, 1)) {
            cur.advance();
            return "-" + cur.advance().lexeme();
        }
        if (cur.match(TokenType.NUMBER)) {
            return cur.advance().lexeme();
        }
        return super.parseLiteralValue(cur);
    }

    /**
     * Performs a look-ahead test to determine whether this parser is applicable.
     *
     * @param cur the current cursor pointing to the next token to be parsed
     * @param ctx the parsing context providing configuration, helpers and nested parsing
     * @return {@code true} if this parser should be used, otherwise {@code false}
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.IDENT)
            && cur.peek().lexeme().equalsIgnoreCase("interval")
            && cur.matchAny(1, TokenType.STRING, TokenType.NUMBER);
    }
}
