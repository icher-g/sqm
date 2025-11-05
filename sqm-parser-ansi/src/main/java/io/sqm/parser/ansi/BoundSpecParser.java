package io.sqm.parser.ansi;

import io.sqm.core.BoundSpec;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class BoundSpecParser implements Parser<BoundSpec> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<BoundSpec> parse(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.UNBOUNDED)) {
            if (cur.match(TokenType.PRECEDING, 1)) {
                var r = ctx.parse(BoundSpec.UnboundedPreceding.class, cur);
                return finalize(cur, ctx, r);
            }
            var r = ctx.parse(BoundSpec.UnboundedFollowing.class, cur);
            return finalize(cur, ctx, r);
        }
        if (cur.match(TokenType.CURRENT)) {
            var r = ctx.parse(BoundSpec.CurrentRow.class, cur);
            return finalize(cur, ctx, r);
        }
        if (cur.match(TokenType.PRECEDING, 1)) {
            var r = ctx.parse(BoundSpec.Preceding.class, cur);
            return finalize(cur, ctx, r);
        }
        var r = ctx.parse(BoundSpec.Following.class, cur);
        return finalize(cur, ctx, r);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<BoundSpec> targetType() {
        return BoundSpec.class;
    }
}
