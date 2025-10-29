package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.Predicate;
import io.sqm.core.WhenThen;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class WhenThenParser implements Parser<WhenThen> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<WhenThen> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected WHEN <predicate>", TokenType.WHEN);

        var fr = ctx.parse(Predicate.class, cur);
        if (fr.isError()) {
            return error(fr);
        }

        // THEN
        cur.expect("Expected THEN after WHEN <predicate>", TokenType.THEN);

        // <result>
        var thenResult = ctx.parse(Expression.class, cur);
        if (thenResult.isError()) {
            return error(thenResult);
        }
        return ok(WhenThen.of(fr.value(), thenResult.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<WhenThen> targetType() {
        return WhenThen.class;
    }
}
