package io.sqm.parser.ansi;

import io.sqm.core.OverSpec;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;

/**
 * Dispatches parsing for {@code OVER} specifications.
 */
public class OverSpecParser implements Parser<OverSpec> {
    /**
     * Creates an over-spec parser.
     */
    public OverSpecParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends OverSpec> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected OVER", TokenType.OVER);
        if (cur.match(TokenType.IDENT)) {
            return ctx.parse(OverSpec.Ref.class, cur);
        }
        else {
            cur.expect("Expected '(' after OVER", TokenType.LPAREN);
            var or = ctx.parse(OverSpec.Def.class, cur);
            if (or.isError()) {
                return error(or);
            }
            cur.expect("Expected ')' to close statement", TokenType.RPAREN);
            return or;
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OverSpec> targetType() {
        return OverSpec.class;
    }
}
