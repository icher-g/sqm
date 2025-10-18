package io.cherlabs.sqm.parser.ansi;

import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.TokenType;
import io.cherlabs.sqm.parser.spi.Lookups;

import java.util.Set;

/**
 * A default implementation of the lookups.
 */
public class AnsiLookups implements Lookups {

    @Override
    public boolean looksLikeStar(Cursor cur) {
        return cur.match(TokenType.STAR);
    }

    @Override
    public boolean looksLikeCase(Cursor cur) {
        return cur.match(TokenType.CASE);
    }

    @Override
    public boolean looksLikeColumn(Cursor cur) {
        if (cur.match(TokenType.IDENT)) {
            int i = 1;
            while (cur.pos() + i + 1 < cur.size() && cur.match(TokenType.DOT, i)) {
                if (!cur.match(TokenType.IDENT, i + 1)) {
                    return false;
                }
                i++;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean looksLikeValue(Cursor cur) {
        return looksLikeValue(cur, 0);
    }

    /**
     * Returns true if at idx we have IDENT ('.' IDENT)* '(' — i.e., a function call begins.
     */
    @Override
    public boolean looksLikeFunction(Cursor cur) {
        if (!cur.match(TokenType.IDENT)) {
            return false;
        }
        int p = 1;
        while (cur.match(TokenType.DOT, p) && cur.match(TokenType.IDENT, p + 1)) {
            p += 2;
        }
        return cur.match(TokenType.LPAREN, p);
    }

    @Override
    public boolean looksLikeWithQuery(Cursor cur) {
        return cur.match(TokenType.WITH);
    }

    @Override
    public boolean looksLikeCompositeQuery(Cursor cur) {
        return cur.find(Indicators.COMPOSITE_QUERY_INDICATORS) < cur.size();
    }

    @Override
    public boolean looksLikeSubquery(Cursor cur) {
        if (cur.match(TokenType.LPAREN)) {
            return cur.match(TokenType.SELECT, 1);
        }
        return cur.match(TokenType.SELECT);
    }

    @Override
    public boolean looksLikeCompositeFilter(Cursor cur) {
        var i = cur.find(Indicators.COMPOSITE_FILTER_INDICATORS, Set.of(TokenType.BETWEEN, TokenType.LPAREN), Set.of(TokenType.AND, TokenType.RPAREN), 0);
        if (i < cur.size()) {
            var lookahead = i - cur.pos();
            if (cur.match(TokenType.NOT, lookahead)) { // avoid recognizing NOT IN as a NOT unary operation.
                return !cur.match(TokenType.IN, lookahead + 1);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean looksLikeTupleFilter(Cursor cur) {
        // match (t.c,
        if (cur.match(TokenType.LPAREN)) {
            if (cur.match(TokenType.IDENT, 1)) {
                int i = 2;
                while (cur.pos() + i < cur.size() && cur.match(TokenType.DOT, i)) {
                    if (!cur.match(TokenType.IDENT, i)) {
                        return false;
                    }
                    i++;
                }
                return cur.match(TokenType.COMMA, i);
            }
        }
        return false;
    }

    @Override
    public boolean looksLikeListValues(Cursor cur) {
        // match (1, or ('a', --> (1,2)
        return cur.match(TokenType.LPAREN) &&
            cur.pos() < cur.size() - 1 &&
            looksLikeValue(cur, 1);
    }

    @Override
    public boolean looksLikeTupleValues(Cursor cur) {
        // match ((1, or (('a', --> ((1,2),(3,4))
        return cur.pos() < cur.size() - 3 &&
            cur.match(TokenType.LPAREN) &&
            cur.match(TokenType.LPAREN, 1) &&
            looksLikeValue(cur, 2) &&
            cur.match(TokenType.COMMA, 3);
    }

    @Override
    public boolean looksLikeRangeValues(Cursor cur) {
        return looksLikeValue(cur, 0) && cur.pos() + 2 < cur.size() && cur.match(TokenType.AND, 1) && looksLikeValue(cur, 2);
    }

    private boolean looksLikeValue(Cursor cur, int lookahead) {
        return cur.matchAny(lookahead, TokenType.NUMBER, TokenType.STRING, TokenType.FALSE, TokenType.TRUE, TokenType.NULL);
    }
}
