package io.sqm.parser.sqlserver;

import io.sqm.core.MergeDeleteAction;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses SQL Server {@code MERGE ... WHEN MATCHED THEN DELETE} actions.
 */
public class MergeDeleteActionParser extends io.sqm.parser.ansi.MergeDeleteActionParser {

    /**
     * Creates a SQL Server merge-delete-action parser.
     */
    public MergeDeleteActionParser() {
    }

    @Override
    public ParseResult<? extends MergeDeleteAction> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected DELETE", TokenType.DELETE);
        return ok(MergeDeleteAction.of());
    }
}
