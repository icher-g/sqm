package io.sqm.parser.oracle;

import io.sqm.core.ResultClause;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Parses Oracle {@code DELETE} statements with {@code RETURNING ... INTO}.
 */
public class DeleteStatementParser extends io.sqm.parser.ansi.DeleteStatementParser {

    /**
     * Creates an Oracle delete-statement parser.
     */
    public DeleteStatementParser() {
    }

    @Override
    protected ParseResult<ResultClause> parseReturning(Cursor cur, ParseContext ctx) {
        return OracleParserSupport.parseReturningInto(this, cur, ctx, "DELETE ... RETURNING INTO");
    }
}
