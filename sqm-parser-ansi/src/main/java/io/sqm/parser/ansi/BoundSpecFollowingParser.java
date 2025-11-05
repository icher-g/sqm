package io.sqm.parser.ansi;

import io.sqm.core.BoundSpec;
import io.sqm.core.LiteralExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class BoundSpecFollowingParser implements Parser<BoundSpec.Following> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<BoundSpec.Following> parse(Cursor cur, ParseContext ctx) {
        var er = ctx.parse(LiteralExpr.class, cur);
        if (er.isError()) {
            return error(er);
        }
        cur.expect("Expected FOLLOWING after expression", TokenType.FOLLOWING);
        return finalize(cur, ctx, BoundSpec.following(er.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<BoundSpec.Following> targetType() {
        return BoundSpec.Following.class;
    }
}
