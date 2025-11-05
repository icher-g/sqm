package io.sqm.parser.ansi;

import io.sqm.core.NotPredicate;
import io.sqm.core.Predicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class NotPredicateParser implements Parser<NotPredicate> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<NotPredicate> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected NOT", TokenType.NOT);
        cur.expect("Expected (", TokenType.LPAREN);

        var res = ctx.parse(Predicate.class, cur);
        if (res.isError()) {
            return error(res);
        }

        cur.expect("Expected )", TokenType.RPAREN);
        return finalize(cur, ctx, NotPredicate.of(res.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<NotPredicate> targetType() {
        return NotPredicate.class;
    }
}
