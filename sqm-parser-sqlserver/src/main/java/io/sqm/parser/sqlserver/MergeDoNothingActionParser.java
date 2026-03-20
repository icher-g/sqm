package io.sqm.parser.sqlserver;

import io.sqm.core.MergeDoNothingAction;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;

/**
 * Parses SQL Server {@code MERGE ... THEN DO NOTHING} actions as explicit unsupported syntax.
 */
public class MergeDoNothingActionParser extends io.sqm.parser.ansi.MergeDoNothingActionParser {

    /**
     * Creates a SQL Server merge-do-nothing-action parser.
     */
    public MergeDoNothingActionParser() {
    }

    @Override
    public ParseResult<? extends MergeDoNothingAction> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected DO", TokenType.DO);
        cur.expect("Expected NOTHING after DO", TokenType.NOTHING);
        return error("SQL Server MERGE DO NOTHING actions are not supported", cur.fullPos());
    }
}
