package io.sqm.parser.oracle;

import io.sqm.core.MergeClause;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Parses Oracle {@code WHEN ... THEN ...} MERGE clauses.
 */
public class MergeClauseParser extends io.sqm.parser.ansi.MergeClauseParser {

    /**
     * Creates an Oracle merge-clause parser.
     */
    public MergeClauseParser() {
    }

    @Override
    public ParseResult<? extends MergeClause> parse(Cursor cur, ParseContext ctx) {
        return parseSupportedClause(cur, ctx);
    }

    @Override
    protected String notMatchedByUnsupportedMessage() {
        return "Oracle MERGE does not support WHEN NOT MATCHED BY SOURCE";
    }
}
