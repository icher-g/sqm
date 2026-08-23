package io.sqm.parser.ansi;

import io.sqm.core.AfterMatchSkip;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses an {@code AFTER MATCH SKIP} clause.
 */
public class AfterMatchSkipParser implements Parser<AfterMatchSkip> {
    /**
     * Creates an after-match-skip parser.
     */
    public AfterMatchSkipParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends AfterMatchSkip> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected AFTER", TokenType.AFTER);
        cur.expect("Expected MATCH after AFTER", TokenType.MATCH);
        cur.expect("Expected SKIP after AFTER MATCH", TokenType.SKIP);
        if (cur.consumeIf(TokenType.PAST)) {
            cur.expect("Expected LAST after PAST", TokenType.LAST);
            cur.expect("Expected ROW after PAST LAST", TokenType.ROW);
            return ok(AfterMatchSkip.of(AfterMatchSkip.Kind.PAST_LAST_ROW, AfterMatchSkip.Position.DEFAULT, null));
        }
        cur.expect("Expected TO after AFTER MATCH SKIP", TokenType.TO);
        if (cur.consumeIf(TokenType.NEXT)) {
            cur.expect("Expected ROW after TO NEXT", TokenType.ROW);
            return ok(AfterMatchSkip.of(AfterMatchSkip.Kind.TO_NEXT_ROW, AfterMatchSkip.Position.DEFAULT, null));
        }
        var position = AfterMatchSkip.Position.DEFAULT;
        if (cur.consumeIf(TokenType.FIRST)) position = AfterMatchSkip.Position.FIRST;
        else
            if (cur.consumeIf(TokenType.LAST)) position = AfterMatchSkip.Position.LAST;
        var variable = toIdentifier(cur.expect("Expected pattern variable after TO", TokenType.IDENT));
        return ok(AfterMatchSkip.of(AfterMatchSkip.Kind.TO_VARIABLE, position, variable));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<AfterMatchSkip> targetType() {
        return AfterMatchSkip.class;
    }
}
