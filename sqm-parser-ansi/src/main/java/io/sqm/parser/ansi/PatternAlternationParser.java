package io.sqm.parser.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses a row-pattern alternation.
 */
public class PatternAlternationParser implements Parser<MatchPattern.Alternation> {
    /**
     * Creates a pattern-alternation parser.
     */
    public PatternAlternationParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends MatchPattern.Alternation> parse(Cursor cur, ParseContext ctx) {
        var parsed = PatternGrammar.parseAlternation(cur, ctx);
        if (parsed.isError()) return error(parsed);
        return parsed.value() instanceof MatchPattern.Alternation alternation
            ? ok(alternation) : error("Expected pattern alternation", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchPattern.Alternation> targetType() {
        return MatchPattern.Alternation.class;
    }
}
