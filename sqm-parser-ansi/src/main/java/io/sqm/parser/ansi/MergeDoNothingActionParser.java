package io.sqm.parser.ansi;

import io.sqm.core.MergeDoNothingAction;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Baseline ANSI parser for {@link MergeDoNothingAction}.
 */
public class MergeDoNothingActionParser implements Parser<MergeDoNothingAction> {

    /**
     * Creates a merge-do-nothing-action parser.
     */
    public MergeDoNothingActionParser() {
    }

    @Override
    public ParseResult<? extends MergeDoNothingAction> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected DO", TokenType.DO);
        cur.expect("Expected NOTHING after DO", TokenType.NOTHING);
        return ParseResult.error("MERGE DO NOTHING actions are not supported by this dialect", cur.fullPos());
    }

    @Override
    public Class<MergeDoNothingAction> targetType() {
        return MergeDoNothingAction.class;
    }
}
