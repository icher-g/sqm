package io.sqm.parser.ansi;

import io.sqm.core.MergeInsertAction;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Baseline ANSI parser for {@link MergeInsertAction}.
 */
public class MergeInsertActionParser implements Parser<MergeInsertAction> {

    /**
     * Creates a merge-insert-action parser.
     */
    public MergeInsertActionParser() {
    }

    @Override
    public ParseResult<? extends MergeInsertAction> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected INSERT", TokenType.INSERT);
        return ParseResult.error("MERGE INSERT actions are not supported by this dialect", cur.fullPos());
    }

    @Override
    public Class<MergeInsertAction> targetType() {
        return MergeInsertAction.class;
    }
}
