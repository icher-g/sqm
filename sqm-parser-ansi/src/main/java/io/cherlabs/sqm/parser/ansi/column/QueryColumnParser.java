package io.cherlabs.sqm.parser.ansi.column;

import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.QueryColumn;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.TokenType;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

public class QueryColumnParser implements Parser<QueryColumn> {

    @Override
    public ParseResult<QueryColumn> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected '(' before SELECT subquery", TokenType.LPAREN);

        var subCur = cur.advance(cur.find(TokenType.RPAREN));
        var body = ctx.parse(Query.class, subCur);
        if (body.isError()) {
            return error(body);
        }

        cur.expect("Expected ')' before SELECT subquery", TokenType.RPAREN);

        var alias = parseAlias(cur);
        return ok(new QueryColumn(body.value(), alias));
    }

    @Override
    public Class<QueryColumn> targetType() {
        return QueryColumn.class;
    }
}
