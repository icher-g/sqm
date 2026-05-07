package io.sqm.parser.postgresql;

import io.sqm.core.MergeInsertAction;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Parses PostgreSQL {@code MERGE ... WHEN NOT MATCHED THEN INSERT ... VALUES (...)} actions.
 */
public class MergeInsertActionParser extends io.sqm.parser.ansi.MergeInsertActionParser {

    /**
     * Creates a PostgreSQL merge-insert-action parser.
     */
    public MergeInsertActionParser() {
    }

    @Override
    public ParseResult<? extends MergeInsertAction> parse(Cursor cur, ParseContext ctx) {
        return parseSupportedAction(cur, ctx);
    }
}
