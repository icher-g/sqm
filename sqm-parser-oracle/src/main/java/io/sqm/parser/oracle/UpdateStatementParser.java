package io.sqm.parser.oracle;

import io.sqm.core.ResultClause;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Parses Oracle {@code UPDATE} statements with {@code RETURNING ... INTO}.
 */
public class UpdateStatementParser extends io.sqm.parser.ansi.UpdateStatementParser {

    /**
     * Creates an Oracle update-statement parser.
     */
    public UpdateStatementParser() {
    }

    @Override
    protected ParseResult<ResultClause> parseReturning(Cursor cur, ParseContext ctx) {
        return OracleParserSupport.parseReturningInto(this, cur, ctx, "UPDATE ... RETURNING INTO");
    }
}
