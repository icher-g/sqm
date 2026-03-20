package io.sqm.parser.ansi;

import io.sqm.core.MergeStatement;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Baseline ANSI parser for {@code MERGE} statements.
 */
public class MergeStatementParser implements Parser<MergeStatement> {

    /**
     * Creates a merge-statement parser.
     */
    public MergeStatementParser() {
    }

    @Override
    public ParseResult<? extends MergeStatement> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected MERGE", TokenType.MERGE);
        return ParseResult.error("MERGE is not supported by this dialect", cur.fullPos());
    }

    @Override
    public Class<MergeStatement> targetType() {
        return MergeStatement.class;
    }
}
