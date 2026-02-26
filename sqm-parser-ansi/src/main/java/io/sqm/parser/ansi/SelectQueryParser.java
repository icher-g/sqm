package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses SELECT query statements.
 */
public class SelectQueryParser implements Parser<SelectQuery> {
    /**
     * Creates a select-query parser.
     */
    public SelectQueryParser() {
    }

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

        var q = SelectQuery.builder();

        // DISTINCT
        if (cur.match(TokenType.DISTINCT)) {
            var dr = ctx.parse(DistinctSpec.class, cur);
            if (dr.isError()) {
                return error(dr);
            }
            q.distinct(dr.value());
        }

        // SELECT list
        var items = parseItems(SelectItem.class, cur, ctx);
        if (items.isError()) {
            return error(items);
        }
        q.select(items.value());

        // FROM (optional)
        if (cur.consumeIf(TokenType.FROM)) {
            var tr = ctx.parse(TableRef.class, cur);
            if (tr.isError()) {
                return error(tr);
            }
            q.from(tr.value());

            // CROSS JOIN with ','
            while (cur.consumeIf(TokenType.COMMA)) {
                var cj = ctx.parse(TableRef.class, cur);
                if (cj.isError()) {
                    return error(cj);
                }
                q.join(CrossJoin.of(cj.value()));
            }

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

        // WINDOW
        while (cur.consumeIf(TokenType.WINDOW)) {
            do {
                var wr = ctx.parse(WindowDef.class, cur);
                if (wr.isError()) {
                    return error(wr);
                }
                q.window(wr.value());
            }
            while (cur.consumeIf(TokenType.COMMA)); // WINDOW w1 AS (PARTITION BY dept ORDER BY salary DESC), w2 AS (ORDER BY salary DESC);
        }

        // ORDER BY (optional)
        if (cur.match(TokenType.ORDER) && cur.match(TokenType.BY, 1)) {
            var obr = ctx.parse(OrderBy.class, cur);
            if (obr.isError()) {
                return error(obr);
            }
            q.orderBy(obr.value().items());
        }

        // LIMIT & OFFSET (optional)
        var lor = ctx.parse(LimitOffset.class, cur);
        if (lor.isError()) {
            return error(lor);
        }
        var lo = lor.value();
        if (lo.limit() != null || lo.offset() != null || lo.limitAll()) {
            q.limitOffset(lo);
        }

        // Locking clause (FOR UPDATE, FOR SHARE, etc.)
        if (cur.match(TokenType.FOR)) {
            var lockFor = ctx.parse(LockingClause.class, cur);
            if (lockFor.isError()) {
                return error(lockFor);
            }
            q.lockFor(lockFor.value());
        }

        return ok(q.build());
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
