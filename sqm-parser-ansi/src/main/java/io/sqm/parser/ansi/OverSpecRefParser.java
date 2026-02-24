package io.sqm.parser.ansi;

import io.sqm.core.OverSpec;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.ok;

public class OverSpecRefParser implements Parser<OverSpec.Ref> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<OverSpec.Ref> parse(Cursor cur, ParseContext ctx) {
        var t = cur.expect("Expected IDENTIFIER after OVER", TokenType.IDENT);
        return ok(OverSpec.ref(toIdentifier(t)));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OverSpec.Ref> targetType() {
        return OverSpec.Ref.class;
    }
}
