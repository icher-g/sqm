package io.sqm.parser.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses a row-pattern primary with an optional quantifier.
 */
public class QuantifiedPatternParser implements Parser<MatchPattern.Quantified> {
    /**
     * Creates a quantified-pattern parser.
     */
    public QuantifiedPatternParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends MatchPattern.Quantified> parse(Cursor cur, ParseContext ctx) {
        var parsed = PatternGrammar.parseQuantified(cur, ctx);
        if (parsed.isError()) return error(parsed);
        return parsed.value() instanceof MatchPattern.Quantified quantified
            ? ok(quantified) : error("Expected pattern quantifier", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchPattern.Quantified> targetType() {
        return MatchPattern.Quantified.class;
    }
}
