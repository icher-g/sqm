package io.sqm.parser.sqlserver;

import io.sqm.core.MergeClause;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Parses SQL Server {@code WHEN ... THEN ...} MERGE clauses.
 */
public class MergeClauseParser extends io.sqm.parser.ansi.MergeClauseParser {

    /**
     * Creates a SQL Server merge-clause parser.
     */
    public MergeClauseParser() {
    }

    @Override
    public ParseResult<? extends MergeClause> parse(Cursor cur, ParseContext ctx) {
        return parseSupportedClause(cur, ctx);
    }

    @Override
    protected MergeClause.MatchType parseNotMatchedBy(Cursor cur, ParseContext ctx) {
        cur.expect("Expected SOURCE after WHEN NOT MATCHED BY", TokenType.SOURCE);
        return MergeClause.MatchType.NOT_MATCHED_BY_SOURCE;
    }
}
