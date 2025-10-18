package io.cherlabs.sqm.parser;

import io.cherlabs.sqm.core.CompositeQuery;
import io.cherlabs.sqm.core.Query;
import io.cherlabs.sqm.core.SelectQuery;
import io.cherlabs.sqm.core.WithQuery;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

/**
 * Full query parser.
 */
public final class QueryParser implements Parser<Query> {

    @Override
    public Class<Query> targetType() {
        return Query.class;
    }

    @Override
    public ParseResult<Query> parse(Cursor cur, ParseContext ctx) {
        if (ctx.lookups().looksLikeWithQuery(cur)) {
            var wr = ctx.parse(WithQuery.class, cur);
            return finalize(cur, ctx, wr);
        }

        if (ctx.lookups().looksLikeCompositeQuery(cur)) {
            var cr = ctx.parse(CompositeQuery.class, cur);
            return finalize(cur, ctx, cr);
        }

        var sr = ctx.parse(SelectQuery.class, cur);
        return finalize(cur, ctx, sr);
    }
}
