package io.sqm.parser;

import io.sqm.core.MergeAction;
import io.sqm.core.MergeDeleteAction;
import io.sqm.core.MergeDoNothingAction;
import io.sqm.core.MergeInsertAction;
import io.sqm.core.MergeUpdateAction;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Parser entry point for {@link MergeAction} families.
 */
public class MergeActionParser implements Parser<MergeAction> {

    /**
     * Creates a merge-action parser.
     */
    public MergeActionParser() {
    }

    @Override
    public ParseResult<? extends MergeAction> parse(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.UPDATE)) {
            return ctx.parse(MergeUpdateAction.class, cur);
        }
        if (cur.match(TokenType.DELETE)) {
            return ctx.parse(MergeDeleteAction.class, cur);
        }
        if (cur.match(TokenType.INSERT)) {
            return ctx.parse(MergeInsertAction.class, cur);
        }
        if (cur.match(TokenType.DO)) {
            return ctx.parse(MergeDoNothingAction.class, cur);
        }
        return ParseResult.error("Expected MERGE action", cur.fullPos());
    }

    @Override
    public Class<MergeAction> targetType() {
        return MergeAction.class;
    }
}
