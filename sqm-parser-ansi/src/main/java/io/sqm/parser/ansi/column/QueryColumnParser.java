package io.sqm.parser.ansi.column;

import io.sqm.core.Query;
import io.sqm.core.QueryColumn;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

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
