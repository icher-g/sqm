package io.sqm.parser.ansi;

import io.sqm.core.MatchPattern;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses a row-pattern concatenation.
 */
public class PatternSequenceParser implements Parser<MatchPattern.Sequence> {
    /**
     * Creates a pattern-sequence parser.
     */
    public PatternSequenceParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends MatchPattern.Sequence> parse(Cursor cur, ParseContext ctx) {
        var parsed = PatternGrammar.parseSequence(cur, ctx);
        if (parsed.isError()) return error(parsed);
        return parsed.value() instanceof MatchPattern.Sequence sequence
            ? ok(sequence) : error("Expected at least two pattern elements", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MatchPattern.Sequence> targetType() {
        return MatchPattern.Sequence.class;
    }
}
