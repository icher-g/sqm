package io.sqm.parser.postgresql;

import io.sqm.core.MergeDoNothingAction;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses PostgreSQL {@code MERGE ... THEN DO NOTHING} actions.
 */
public class MergeDoNothingActionParser extends io.sqm.parser.ansi.MergeDoNothingActionParser {

    /**
     * Creates a PostgreSQL merge-do-nothing-action parser.
     */
    public MergeDoNothingActionParser() {
    }

    @Override
    public ParseResult<? extends MergeDoNothingAction> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected DO", TokenType.DO);
        cur.expect("Expected NOTHING after DO", TokenType.NOTHING);
        return ok(MergeDoNothingAction.of());
    }
}
