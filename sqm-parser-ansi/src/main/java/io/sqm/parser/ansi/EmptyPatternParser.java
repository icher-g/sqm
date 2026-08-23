package io.sqm.parser.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses the semantic empty row pattern {@code ()}.
 */
public class EmptyPatternParser implements MatchableParser<MatchPattern.Empty> {
    /**
     * Creates an empty-pattern parser.
     */
    public EmptyPatternParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends MatchPattern.Empty> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected (", TokenType.LPAREN);
        cur.expect("Expected ) for empty pattern", TokenType.RPAREN);
        return ok(MatchPattern.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchPattern.Empty> targetType() {
        return MatchPattern.Empty.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.LPAREN) && cur.match(TokenType.RPAREN, 1);
    }
}
