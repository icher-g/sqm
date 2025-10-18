package io.cherlabs.sqm.parser.ansi.query;

import io.cherlabs.sqm.core.CompositeQuery;
import io.cherlabs.sqm.core.LimitOffset;
import io.cherlabs.sqm.core.OrderBy;
import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.parser.ansi.Indicators;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.Token;
import io.cherlabs.sqm.parser.core.TokenType;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

public class CompositeQueryParser implements Parser<CompositeQuery> {
    @Override
    public ParseResult<CompositeQuery> parse(Cursor cur, ParseContext ctx) {
        List<Query> terms = new ArrayList<>();
        List<CompositeQuery.Op> ops = new ArrayList<>();

        Token t;
        do {
            cur.consumeIf(TokenType.LPAREN); // remove '(' if presented.
            var subCur = cur.advance(cur.find(TokenType.UNION, TokenType.INTERSECT, TokenType.EXCEPT, TokenType.EOF));
            var term = ctx.parse(Query.class, subCur);
            if (term.isError()) {
                return error(term);
            }
            cur.consumeIf(TokenType.RPAREN); // remove ')' if presented.
            terms.add(term.value());
            t = cur.isEof() ? cur.peek() : cur.advance();
            var isAll = cur.consumeIf(TokenType.ALL);
            switch (t.type()) {
                case UNION -> {
                    if (isAll)
                        ops.add(CompositeQuery.Op.unionAll());
                    else
                        ops.add(CompositeQuery.Op.union());
                }
                case INTERSECT -> {
                    if (isAll)
                        ops.add(CompositeQuery.Op.intersectAll());
                    else
                        ops.add(CompositeQuery.Op.intersect());
                }
                case EXCEPT -> {
                    if (isAll)
                        ops.add(CompositeQuery.Op.exceptAll());
                    else
                        ops.add(CompositeQuery.Op.except());
                }
            }
        } while (Indicators.COMPOSITE_QUERY_INDICATORS.contains(t.type()));

        // ORDER BY (optional)
        var obr = ctx.parse(OrderBy.class, cur);
        if (obr.isError()) {
            return error(obr);
        }

        // LIMIT & OFFSET (optional)
        var lor = ctx.parse(LimitOffset.class, cur);
        if (lor.isError()) {
            return error(lor);
        }

        return ok(new CompositeQuery(terms, ops, obr.value(), lor.value()));
    }

    @Override
    public Class<CompositeQuery> targetType() {
        return CompositeQuery.class;
    }
}
