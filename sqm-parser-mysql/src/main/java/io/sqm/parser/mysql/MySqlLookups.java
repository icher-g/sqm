package io.sqm.parser.mysql;

import io.sqm.parser.ansi.AnsiLookups;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.Lookahead;
import io.sqm.parser.core.TokenType;

/**
 * MySQL-specific lookups with support for unquoted numeric {@code INTERVAL}
 * literals such as {@code INTERVAL 1 DAY}.
 */
public class MySqlLookups extends AnsiLookups {
    /**
     * Creates MySQL lookups.
     */
    public MySqlLookups() {
    }

    /**
     * Checks whether the next tokens represent a literal expression.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a literal expression appears ahead, otherwise {@code false}
     */
    @Override
    public boolean looksLikeLiteralExpr(Cursor cur, Lookahead pos) {
        if (looksLikeMySqlIntervalLiteral(cur, pos)) {
            return true;
        }
        return super.looksLikeLiteralExpr(cur, pos);
    }

    /**
     * Checks whether the next tokens represent a column reference.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a column reference appears ahead, otherwise {@code false}
     */
    @Override
    public boolean looksLikeColumnRef(Cursor cur, Lookahead pos) {
        if (looksLikeMySqlIntervalLiteral(cur, Lookahead.at(pos.current()))) {
            return false;
        }
        return super.looksLikeColumnRef(cur, pos);
    }

    private boolean looksLikeMySqlIntervalLiteral(Cursor cur, Lookahead pos) {
        if (!cur.match(TokenType.IDENT, pos.current())) {
            return false;
        }
        var keyword = cur.peek(pos.current()).lexeme();
        if (!keyword.equalsIgnoreCase("interval")) {
            return false;
        }
        if (cur.match(TokenType.OPERATOR, pos.current() + 1)
            && "-".equals(cur.peek(pos.current() + 1).lexeme())
            && cur.match(TokenType.NUMBER, pos.current() + 2)) {
            pos.increment(3);
            return true;
        }
        if (cur.match(TokenType.NUMBER, pos.current() + 1)) {
            pos.increment(2);
            return true;
        }
        return false;
    }
}
