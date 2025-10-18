package io.sqm.parser.ansi.query;

import io.sqm.core.CteQuery;
import io.sqm.core.Query;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;

public class CteQueryParser implements Parser<CteQuery> {

    @Override
    public ParseResult<CteQuery> parse(Cursor cur, ParseContext ctx) {
        var name = cur.expect("Expected CTE name", TokenType.IDENT);
        var aliases = new ArrayList<String>();

        if (cur.consumeIf(TokenType.LPAREN)) {
            do {
                var alias = cur.expect("Expected column name", TokenType.IDENT);
                aliases.add(alias.lexeme());
            } while (cur.consumeIf(TokenType.COMMA));

            cur.expect("Expected ')'", TokenType.RPAREN);
        }

        cur.expect("Expected AS", TokenType.AS);
        cur.expect("Expected '(' before CTE subquery", TokenType.LPAREN);

        var subCur = cur.advance(cur.find(TokenType.RPAREN));
        var body = ctx.parse(Query.class, subCur);
        if (body.isError()) {
            return error(body);
        }

        cur.expect("Expected '(' before CTE subquery", TokenType.RPAREN);
        return ok(new CteQuery(name.lexeme(), body.value(), aliases));
    }

    @Override
    public Class<CteQuery> targetType() {
        return CteQuery.class;
    }
}
