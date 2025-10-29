package io.sqm.parser.ansi;

import io.sqm.core.Query;
import io.sqm.core.QueryTable;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class QueryTableParser implements Parser<QueryTable> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<QueryTable> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected (", TokenType.LPAREN);

        var query = ctx.parse(Query.class, cur);
        if (query.isError()) {
            return error(query);
        }

        cur.expect("Expected )", TokenType.RPAREN);

        var alias = parseAlias(cur);
        return ok(Query.table(query.value()).as(alias));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<QueryTable> targetType() {
        return QueryTable.class;
    }
}
