package io.sqm.parser.oracle;

import io.sqm.core.MergeInsertAction;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Parses Oracle {@code MERGE ... WHEN NOT MATCHED THEN INSERT ... VALUES (...)} actions.
 */
public class MergeInsertActionParser extends io.sqm.parser.ansi.MergeInsertActionParser {

    /**
     * Creates an Oracle merge-insert-action parser.
     */
    public MergeInsertActionParser() {
    }

    @Override
    public ParseResult<? extends MergeInsertAction> parse(Cursor cur, ParseContext ctx) {
        return parseSupportedAction(cur, ctx);
    }
}
