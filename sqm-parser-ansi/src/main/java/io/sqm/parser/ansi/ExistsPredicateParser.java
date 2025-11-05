package io.sqm.parser.ansi;

import io.sqm.core.ExistsPredicate;
import io.sqm.core.Query;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class ExistsPredicateParser implements Parser<ExistsPredicate> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<ExistsPredicate> parse(Cursor cur, ParseContext ctx) {
        var negated = cur.consumeIf(TokenType.NOT);
        cur.expect("Expected EXISTS", TokenType.EXISTS);
        cur.expect("Expected (", TokenType.LPAREN);

        var res = ctx.parse(Query.class, cur);
        if (res.isError()) {
            return error(res);
        }

        cur.expect("Expected )", TokenType.RPAREN);
        return finalize(cur, ctx, ExistsPredicate.of(res.value(), negated));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ExistsPredicate> targetType() {
        return ExistsPredicate.class;
    }
}
