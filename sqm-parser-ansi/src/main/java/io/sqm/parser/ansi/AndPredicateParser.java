package io.sqm.parser.ansi;

import io.sqm.core.AndPredicate;
import io.sqm.core.Predicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class AndPredicateParser implements Parser<AndPredicate> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<AndPredicate> parse(Cursor cur, ParseContext ctx) {
        if (cur.consumeIf(TokenType.LPAREN)) {
            var res = parseAnd(cur, ctx);
            cur.expect("Expected )", TokenType.RPAREN);
            return res;
        }
        return parseAnd(cur, ctx);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<AndPredicate> targetType() {
        return AndPredicate.class;
    }

    private ParseResult<AndPredicate> parseAnd(Cursor cur, ParseContext ctx) {
        // extract the expression before the AND operator to avoid recursive parsing.
        var exprCur = cur.advance(cur.find(TokenType.AND));
        var lhs = ctx.parse(Predicate.class, exprCur);
        if (lhs.isError()) {
            return error(lhs);
        }

        cur.expect("Expected AND", TokenType.AND);

        var rhs = ctx.parse(Predicate.class, cur);
        if (rhs.isError()) {
            return error(rhs);
        }
        return ok(AndPredicate.of(lhs.value(), rhs.value()));
    }
}
