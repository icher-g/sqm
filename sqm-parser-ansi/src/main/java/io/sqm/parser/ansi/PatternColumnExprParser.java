package io.sqm.parser.ansi;

import io.sqm.core.PatternColumnExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses a pattern-variable-qualified column reference.
 */
public class PatternColumnExprParser implements MatchableParser<PatternColumnExpr> {
    /**
     * Creates a pattern-column parser.
     */
    public PatternColumnExprParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends PatternColumnExpr> parse(Cursor cur, ParseContext ctx) {
        var variable = toIdentifier(cur.expect("Expected pattern variable", TokenType.IDENT));
        cur.expect("Expected . after pattern variable", TokenType.DOT);
        var column = toIdentifier(cur.expect("Expected column after pattern variable", TokenType.IDENT));
        return ok(PatternColumnExpr.of(variable, column));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PatternColumnExpr> targetType() {
        return PatternColumnExpr.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.IDENT) && cur.match(TokenType.DOT, 1) && cur.match(TokenType.IDENT, 2);
    }
}
