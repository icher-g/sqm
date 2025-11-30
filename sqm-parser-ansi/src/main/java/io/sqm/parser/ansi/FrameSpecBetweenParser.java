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

public class FrameSpecBetweenParser implements Parser<FrameSpec.Between> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<FrameSpec.Between> parse(Cursor cur, ParseContext ctx) {
        var t = cur.expect("Expected ROWS | RANGE | GROUPS", TokenType.ROWS, TokenType.RANGE, TokenType.GROUPS);
        var unit = Enum.valueOf(FrameSpec.Unit.class, t.lexeme());
        cur.expect("Expected BETWEEN", TokenType.BETWEEN);
        var start = ctx.parse(BoundSpec.class, cur);
        if (start.isError()) {
            return error(start);
        }
        cur.expect("Expected AND", TokenType.AND);
        var end = ctx.parse(BoundSpec.class, cur);
        if (end.isError()) {
            return error(end);
        }
        return ok(FrameSpec.between(unit, start.value(), end.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<FrameSpec.Between> targetType() {
        return FrameSpec.Between.class;
    }
}
