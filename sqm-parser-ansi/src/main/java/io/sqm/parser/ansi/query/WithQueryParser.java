package io.sqm.parser.ansi.query;

import io.sqm.core.CteQuery;
import io.sqm.core.Query;
import io.sqm.core.WithQuery;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

public class WithQueryParser implements Parser<WithQuery> {

    @Override
    public ParseResult<WithQuery> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected WITH.", TokenType.WITH);

        final boolean recursive = cur.consumeIf(TokenType.RECURSIVE);

        List<CteQuery> ctes = new ArrayList<>();
        do {
            var cte = ctx.parse(CteQuery.class, cur);
            if (cte.isError()) {
                return error(cte);
            }
            ctes.add(cte.value());
        } while (cur.consumeIf(TokenType.COMMA));

        var body = ctx.parse(Query.class, cur);
        if (body.isError()) {
            return error(body);
        }
        return ok(new WithQuery(body.value(), ctes, recursive));
    }

    @Override
    public Class<WithQuery> targetType() {
        return WithQuery.class;
    }
}
