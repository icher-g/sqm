package io.sqm.parser.ansi;

import io.sqm.core.BoundSpec;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Dispatches parsing for supported frame bound specifications.
 */
public class BoundSpecParser implements Parser<BoundSpec> {
    /**
     * Creates a frame bound parser.
     */
    public BoundSpecParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends BoundSpec> parse(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.UNBOUNDED)) {
            if (cur.match(TokenType.PRECEDING, 1)) {
                return ctx.parse(BoundSpec.UnboundedPreceding.class, cur);
            }
            return ctx.parse(BoundSpec.UnboundedFollowing.class, cur);
        }
        if (cur.match(TokenType.CURRENT)) {
            return ctx.parse(BoundSpec.CurrentRow.class, cur);
        }
        if (cur.match(TokenType.PRECEDING, 1)) {
            return ctx.parse(BoundSpec.Preceding.class, cur);
        }
        return ctx.parse(BoundSpec.Following.class, cur);
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
