package io.sqm.parser.sqlserver;

import io.sqm.core.ResultClause;
import io.sqm.core.ResultInto;
import io.sqm.core.ResultItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses SQL Server-style {@code OUTPUT} clauses.
 */
public class ResultClauseParser implements Parser<ResultClause> {

    /**
     * Creates an result-clause parser.
     */
    public ResultClauseParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<ResultClause> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected OUTPUT", TokenType.OUTPUT);

        var items = parseItems(ResultItem.class, cur, ctx);
        if (items.isError()) {
            return error(items);
        }

        ResultInto into = null;
        
        if (cur.match(TokenType.INTO)) {
            var parsedInto = ctx.parse(ResultInto.class, cur);
            if (parsedInto.isError()) {
                return error(parsedInto);
            }
            into = parsedInto.value();
        }

        return ok(ResultClause.of(items.value(), into));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ResultClause> targetType() {
        return ResultClause.class;
    }
}
