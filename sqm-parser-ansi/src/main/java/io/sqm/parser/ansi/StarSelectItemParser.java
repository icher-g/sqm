package io.sqm.parser.ansi;

import io.sqm.core.SelectItem;
import io.sqm.core.StarSelectItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class StarSelectItemParser implements Parser<StarSelectItem> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<StarSelectItem> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected '*'", TokenType.STAR);
        return finalize(cur, ctx, SelectItem.star());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<StarSelectItem> targetType() {
        return StarSelectItem.class;
    }
}
