package io.sqm.parser.ansi;

import io.sqm.core.MergeClause;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Baseline ANSI parser for {@link MergeClause}.
 */
public class MergeClauseParser implements Parser<MergeClause> {

    /**
     * Creates a merge-clause parser.
     */
    public MergeClauseParser() {
    }

    @Override
    public ParseResult<? extends MergeClause> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected WHEN", TokenType.WHEN);
        return ParseResult.error("MERGE clauses are not supported by this dialect", cur.fullPos());
    }

    @Override
    public Class<MergeClause> targetType() {
        return MergeClause.class;
    }
}
