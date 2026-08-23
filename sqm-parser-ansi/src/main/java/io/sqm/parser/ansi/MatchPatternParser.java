package io.sqm.parser.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Parses a complete precedence-aware row pattern.
 */
public class MatchPatternParser implements Parser<MatchPattern> {
    /**
     * Creates a match-pattern parser.
     */
    public MatchPatternParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends MatchPattern> parse(Cursor cur, ParseContext ctx) {
        return PatternGrammar.parseAlternation(cur, ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchPattern> targetType() {
        return MatchPattern.class;
    }
}
