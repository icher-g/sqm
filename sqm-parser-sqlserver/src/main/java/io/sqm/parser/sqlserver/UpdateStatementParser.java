package io.sqm.parser.sqlserver;

import io.sqm.core.ResultClause;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses SQL Server {@code UPDATE} statements with {@code OUTPUT}.
 */
public class UpdateStatementParser extends io.sqm.parser.ansi.UpdateStatementParser {

    /**
     * Creates a SQL Server update parser.
     */
    public UpdateStatementParser() {
    }

    /**
     * Parses optional {@code OUTPUT} clause.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return parsed result clause or {@code null} when omitted
     */
    @Override
    protected ParseResult<ResultClause> parseOutput(Cursor cur, ParseContext ctx) {
        if (!cur.match(TokenType.OUTPUT)) {
            return ok(null);
        }

        var output = ctx.parse(ResultClause.class, cur);
        if (output.isError()) {
            return error(output);
        }
        return ok(output.value());
    }
}