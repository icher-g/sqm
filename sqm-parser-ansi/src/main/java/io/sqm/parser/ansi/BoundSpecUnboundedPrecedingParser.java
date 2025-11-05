package io.sqm.parser.ansi;

import io.sqm.core.BoundSpec;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class BoundSpecUnboundedPrecedingParser implements Parser<BoundSpec.UnboundedPreceding> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<BoundSpec.UnboundedPreceding> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected UNBOUNDED", TokenType.UNBOUNDED);
        cur.expect("Expected PRECEDING after UNBOUNDED", TokenType.PRECEDING);
        return finalize(cur, ctx, BoundSpec.unboundedPreceding());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<BoundSpec.UnboundedPreceding> targetType() {
        return BoundSpec.UnboundedPreceding.class;
    }
}
