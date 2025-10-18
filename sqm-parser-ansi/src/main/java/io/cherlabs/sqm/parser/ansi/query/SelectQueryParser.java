package io.cherlabs.sqm.parser.ansi.query;

import io.cherlabs.sqm.core.*;
import io.cherlabs.sqm.parser.ansi.Terminators;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.TokenType;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

public class SelectQueryParser implements Parser<SelectQuery> {

    @Override
    public ParseResult<SelectQuery> parse(Cursor cur, ParseContext ctx) {
        // SELECT
        cur.expect("Expected SELECT at the beginning of a query", TokenType.SELECT);

        var q = new SelectQuery();

        // TOP
        if (cur.consumeIf(TokenType.TOP)) { // T-SQL
            cur.consumeIf(TokenType.LPAREN); // skip '(' if number is wrapped.
            var t = cur.expect("Expected number", TokenType.NUMBER);
            var top = Long.parseLong(t.lexeme());
            q.limit(top);
            cur.consumeIf(TokenType.RPAREN);
        }

        // DISTINCT
        if (cur.consumeIf(TokenType.DISTINCT)) {
            q.distinct(true);
        }

        // SELECT list
        var selCur = cur.advance(cur.find(Terminators.SELECT_TERMINATORS));
        while (!selCur.isEof()) {
            Cursor itemCur = selCur.advance(selCur.find(Terminators.ITEM_TERMINATORS));
            var cr = ctx.parse(Column.class, itemCur);
            if (!cr.ok()) {
                return error(cr);
            }
            if (!itemCur.isEof()) {
                return error("Expected EOF but found: " + itemCur.peek().lexeme(), selCur.fullPos());
            }
            q.select(cr.value());
            selCur.consumeIf(TokenType.COMMA); // skip comma if present
        }

        // FROM (optional)
        if (cur.consumeIf(TokenType.FROM)) {
            var fromCur = cur.advance(cur.find(Terminators.FROM_OR_JOIN_TERMINATORS));
            var tr = ctx.parse(Table.class, fromCur);
            if (tr.isError()) {
                return error(tr);
            }
            q.from(tr.value());

            // JOINs (0..n)
            while (cur.matchAny(TokenType.JOIN, TokenType.INNER, TokenType.LEFT, TokenType.RIGHT, TokenType.FULL, TokenType.CROSS)) {
                int i = 0;
                // calc how many tokens need to be skipped.
                while (cur.matchAny(i, TokenType.JOIN, TokenType.INNER, TokenType.LEFT, TokenType.RIGHT, TokenType.FULL, TokenType.CROSS)) {
                    i++;
                }
                var joinCur = cur.advance(cur.find(Terminators.FROM_OR_JOIN_TERMINATORS, i));
                var jr = ctx.parse(Join.class, joinCur);
                if (jr.isError()) {
                    return error(jr);
                }
                q.join(jr.value());
            }
        }

        // WHERE (optional)
        if (cur.consumeIf(TokenType.WHERE)) {
            var whereCur = cur.advance(cur.find(Terminators.WHERE_TERMINATORS));
            var fr = ctx.parse(Filter.class, whereCur);
            if (fr.isError()) {
                return error(fr);
            }
            q.where(fr.value());
        }

        // GROUP BY (optional)
        var gbr = ctx.parse(GroupBy.class, cur);
        if (gbr.isError()) {
            return error(gbr);
        }
        q.groupBy(gbr.value().items());

        // HAVING (optional)
        if (cur.consumeIf(TokenType.HAVING)) {
            var havingCur = cur.advance(cur.find(Terminators.HAVING_TERMINATORS));
            var hr = ctx.parse(Filter.class, havingCur);
            if (hr.isError()) {
                return error(hr);
            }
            q.having(hr.value());
        }

        // ORDER BY (optional)
        var obr = ctx.parse(OrderBy.class, cur);
        if (obr.isError()) {
            return ParseResult.error(obr.errorMessage(), obr.problems().get(0).pos());
        }
        q.orderBy(obr.value().items());

        // LIMIT & OFFSET (optional)
        var lor = ctx.parse(LimitOffset.class, cur);
        if (lor.isError()) {
            return error(lor);
        }
        if (lor.value().limit() != null) {
            q.limit(lor.value().limit());
        }
        if (lor.value().offset() != null) {
            q.offset(lor.value().offset());
        }

        return ok(q);
    }

    @Override
    public Class<SelectQuery> targetType() {
        return SelectQuery.class;
    }
}
