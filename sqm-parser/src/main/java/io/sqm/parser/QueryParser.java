package io.sqm.parser;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.WithQuery;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class QueryParser implements Parser<Query> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<Query> parse(Cursor cur, ParseContext ctx) {
        if (ctx.lookups().looksLikeWithQuery(cur)) {
            var res = ctx.parse(WithQuery.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeCompositeQuery(cur)) {
            var res = ctx.parse(CompositeQuery.class, cur);
            return finalize(cur, ctx, res);
        }

        var res = ctx.parse(SelectQuery.class, cur);
        return finalize(cur, ctx, res);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<Query> targetType() {
        return Query.class;
    }
}
