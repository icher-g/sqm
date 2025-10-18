package io.cherlabs.sqm.parser.ansi.table;

import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.QueryTable;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.TokenType;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

public class QueryTableParser implements Parser<QueryTable> {
    @Override
    public ParseResult<QueryTable> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected '(' before SELECT subquery", TokenType.LPAREN);

        var subCur = cur.advance(cur.find(TokenType.RPAREN));
        var body = ctx.parse(Query.class, subCur);
        if (body.isError()) {
            return error(body);
        }

        cur.expect("Expected ')' before SELECT subquery", TokenType.RPAREN);

        var alias = parseAlias(cur);
        return ok(new QueryTable(body.value(), alias));
    }

    @Override
    public Class<QueryTable> targetType() {
        return QueryTable.class;
    }
}
