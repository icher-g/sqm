package io.sqm.parser;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class JoinParser implements Parser<Join> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<Join> parse(Cursor cur, ParseContext ctx) {
        if (ctx.lookups().looksLikeCrossJoin(cur)) {
            var res = ctx.parse(CrossJoin.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeNaturalJoin(cur)) {
            var res = ctx.parse(NaturalJoin.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeUsingJoin(cur)) {
            var res = ctx.parse(UsingJoin.class, cur);
            return finalize(cur, ctx, res);
        }

        var res = ctx.parse(OnJoin.class, cur);
        return finalize(cur, ctx, res);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<Join> targetType() {
        return Join.class;
    }
}
