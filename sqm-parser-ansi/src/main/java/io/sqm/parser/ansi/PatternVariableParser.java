package io.sqm.parser.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses one primary pattern-variable occurrence.
 */
public class PatternVariableParser implements MatchableParser<MatchPattern.Variable> {
    /**
     * Creates a pattern-variable parser.
     */
    public PatternVariableParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends MatchPattern.Variable> parse(Cursor cur, ParseContext ctx) {
        return ok(MatchPattern.Variable.of(toIdentifier(cur.expect("Expected pattern variable", TokenType.IDENT))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchPattern.Variable> targetType() {
        return MatchPattern.Variable.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.IDENT);
    }
}
