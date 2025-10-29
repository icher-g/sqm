package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

public class SelectQueryParser implements Parser<SelectQuery> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<SelectQuery> parse(Cursor cur, ParseContext ctx) {
        // SELECT
        cur.expect("Expected SELECT at the beginning of a query", TokenType.SELECT);

        var q = SelectQuery.of();

        // DISTINCT
        if (cur.consumeIf(TokenType.DISTINCT)) {
            q.distinct(true);
        }

        // SELECT list
        List<SelectItem> items = new ArrayList<>();
        do {
            var cr = ctx.parse(SelectItem.class, cur);
            if (!cr.ok()) {
                return error(cr);
            }
            items.add(cr.value());
        } while (cur.consumeIf(TokenType.COMMA));

        q.select(items);

        // FROM (optional)
        if (cur.consumeIf(TokenType.FROM)) {
            var tr = ctx.parse(TableRef.class, cur);
            if (tr.isError()) {
                return error(tr);
            }
            q.from(tr.value());

            // JOINs (0..n)
            while (cur.matchAny(Indicators.JOIN)) {
                var jr = ctx.parse(Join.class, cur);
                if (jr.isError()) {
                    return error(jr);
                }
                q.join(jr.value());
            }
        }

        // WHERE (optional)
        if (cur.consumeIf(TokenType.WHERE)) {
            var fr = ctx.parse(Predicate.class, cur);
            if (fr.isError()) {
                return error(fr);
            }
            q.where(fr.value());
        }

        // GROUP BY (optional)
        if (cur.match(TokenType.GROUP) && cur.match(TokenType.BY, 1)) {
            var gbr = ctx.parse(GroupBy.class, cur);
            if (gbr.isError()) {
                return error(gbr);
            }
            q.groupBy(gbr.value().items());
        }

        // HAVING (optional)
        if (cur.consumeIf(TokenType.HAVING)) {
            var hr = ctx.parse(Predicate.class, cur);
            if (hr.isError()) {
                return error(hr);
            }
            q.having(hr.value());
        }

        // ORDER BY (optional)
        if (cur.match(TokenType.ORDER) && cur.match(TokenType.BY, 1)) {
            var obr = ctx.parse(OrderBy.class, cur);
            if (obr.isError()) {
                return ParseResult.error(obr.errorMessage(), obr.problems().get(0).pos());
            }
            q.orderBy(obr.value().items());
        }

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

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<SelectQuery> targetType() {
        return SelectQuery.class;
    }
}
