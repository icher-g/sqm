package io.sqm.parser.oracle;

import io.sqm.core.ResultClause;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Parses Oracle {@code INSERT} statements with {@code RETURNING ... INTO}.
 */
public class InsertStatementParser extends io.sqm.parser.ansi.InsertStatementParser {

    /**
     * Creates an Oracle insert-statement parser.
     */
    public InsertStatementParser() {
    }

    @Override
    protected ParseResult<ResultClause> parseReturning(Cursor cur, ParseContext ctx) {
        return OracleParserSupport.parseReturningInto(this, cur, ctx, "INSERT ... RETURNING INTO");
    }
}
