package io.sqm.parser.ansi;

import io.sqm.core.MergeUpdateAction;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Baseline ANSI parser for {@link MergeUpdateAction}.
 */
public class MergeUpdateActionParser implements Parser<MergeUpdateAction> {

    /**
     * Creates a merge-update-action parser.
     */
    public MergeUpdateActionParser() {
    }

    @Override
    public ParseResult<? extends MergeUpdateAction> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected UPDATE", TokenType.UPDATE);
        return ParseResult.error("MERGE UPDATE actions are not supported by this dialect", cur.fullPos());
    }

    @Override
    public Class<MergeUpdateAction> targetType() {
        return MergeUpdateAction.class;
    }
}
