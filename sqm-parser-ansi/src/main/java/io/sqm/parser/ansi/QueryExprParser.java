package io.sqm.parser.ansi;

import io.sqm.core.Query;
import io.sqm.core.QueryExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class QueryExprParser implements Parser<QueryExpr> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<QueryExpr> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected (", TokenType.LPAREN);
        var query = ctx.parse(Query.class, cur);
        if (query.isError()) {
            return error(query);
        }
        cur.expect("Expected )", TokenType.RPAREN);
        return finalize(cur, ctx, QueryExpr.of(query.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<QueryExpr> targetType() {
        return QueryExpr.class;
    }
}
