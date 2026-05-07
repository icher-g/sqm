package io.sqm.parser.oracle;

import io.sqm.core.MergeUpdateAction;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Parses Oracle {@code MERGE ... WHEN MATCHED THEN UPDATE SET ...} actions.
 */
public class MergeUpdateActionParser extends io.sqm.parser.ansi.MergeUpdateActionParser {

    /**
     * Creates an Oracle merge-update-action parser.
     */
    public MergeUpdateActionParser() {
    }

    @Override
    public ParseResult<? extends MergeUpdateAction> parse(Cursor cur, ParseContext ctx) {
        return parseSupportedAction(cur, ctx);
    }
}
