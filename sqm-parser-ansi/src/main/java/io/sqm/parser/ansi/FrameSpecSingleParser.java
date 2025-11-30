package io.sqm.parser.ansi;

import io.sqm.core.BoundSpec;
import io.sqm.core.FrameSpec;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class FrameSpecSingleParser implements Parser<FrameSpec.Single> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<FrameSpec.Single> parse(Cursor cur, ParseContext ctx) {
        var t = cur.expect("Expected ROWS | RANGE | GROUPS", TokenType.ROWS, TokenType.RANGE, TokenType.GROUPS);
        var unit = Enum.valueOf(FrameSpec.Unit.class, t.lexeme());
        var bound = ctx.parse(BoundSpec.class, cur);
        if (bound.isError()) {
            return error(bound);
        }
        return ok(FrameSpec.single(unit, bound.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<FrameSpec.Single> targetType() {
        return FrameSpec.Single.class;
    }
}
