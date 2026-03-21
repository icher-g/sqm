package io.sqm.parser.postgresql;

import io.sqm.core.MergeClause;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Parses PostgreSQL {@code WHEN ... THEN ...} MERGE clauses for the first shared slice.
 */
public class MergeClauseParser extends io.sqm.parser.ansi.MergeClauseParser {

    /**
     * Creates a PostgreSQL merge-clause parser.
     */
    public MergeClauseParser() {
    }

    @Override
    public ParseResult<? extends MergeClause> parse(Cursor cur, ParseContext ctx) {
        return parseSupportedClause(cur, ctx);
    }

    @Override
    protected MergeClause.MatchType parseNotMatchedBy(Cursor cur, ParseContext ctx) {
        if (!ctx.capabilities().supports(SqlFeature.MERGE_NOT_MATCHED_BY_SOURCE_CLAUSE)) {
            return null;
        }
        cur.expect("Expected SOURCE after WHEN NOT MATCHED BY", TokenType.SOURCE);
        return MergeClause.MatchType.NOT_MATCHED_BY_SOURCE;
    }

    @Override
    protected String notMatchedByUnsupportedMessage() {
        return "PostgreSQL MERGE ... WHEN NOT MATCHED BY SOURCE ... is not supported by this PostgreSQL version";
    }
}
