package io.sqm.parser.ansi;

import io.sqm.core.FrameSpec;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Dispatches parsing for frame specifications.
 */
public class FrameSpecParser implements Parser<FrameSpec> {
    /**
     * Creates a frame specification parser.
     */
    public FrameSpecParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends FrameSpec> parse(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.BETWEEN, 1)) {
            return ctx.parse(FrameSpec.Between.class, cur);
        }
        return ctx.parse(FrameSpec.Single.class, cur);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<FrameSpec> targetType() {
        return FrameSpec.class;
    }
}
