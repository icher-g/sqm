package io.sqm.parser.ansi;

import io.sqm.core.MergeDeleteAction;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Baseline ANSI parser for {@link MergeDeleteAction}.
 */
public class MergeDeleteActionParser implements Parser<MergeDeleteAction> {

    /**
     * Creates a merge-delete-action parser.
     */
    public MergeDeleteActionParser() {
    }

    @Override
    public ParseResult<? extends MergeDeleteAction> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected DELETE", TokenType.DELETE);
        return ParseResult.error("MERGE DELETE actions are not supported by this dialect", cur.fullPos());
    }

    @Override
    public Class<MergeDeleteAction> targetType() {
        return MergeDeleteAction.class;
    }
}
