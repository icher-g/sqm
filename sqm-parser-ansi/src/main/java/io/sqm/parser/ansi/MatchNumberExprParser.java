package io.sqm.parser.ansi;

import io.sqm.core.MatchNumberExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses the scoped {@code MATCH_NUMBER()} expression.
 */
public class MatchNumberExprParser implements MatchableParser<MatchNumberExpr> {
    /**
     * Creates a match-number parser.
     */
    public MatchNumberExprParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends MatchNumberExpr> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected MATCH_NUMBER", TokenType.IDENT);
        cur.expect("Expected ( after MATCH_NUMBER", TokenType.LPAREN);
        cur.expect("Expected ) after MATCH_NUMBER", TokenType.RPAREN);
        return ok(MatchNumberExpr.of());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchNumberExpr> targetType() {
        return MatchNumberExpr.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.IDENT, "MATCH_NUMBER") && cur.match(TokenType.LPAREN, 1);
    }
}
