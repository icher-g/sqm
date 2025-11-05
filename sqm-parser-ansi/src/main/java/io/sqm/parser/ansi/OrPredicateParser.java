package io.sqm.parser.ansi;

import io.sqm.core.OrPredicate;
import io.sqm.core.Predicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class OrPredicateParser implements Parser<OrPredicate> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<OrPredicate> parse(Cursor cur, ParseContext ctx) {
        if (cur.consumeIf(TokenType.LPAREN)) {
            var res = parseOr(cur, ctx);
            cur.expect("Expected )", TokenType.RPAREN);
            return res;
        }
        return parseOr(cur, ctx);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OrPredicate> targetType() {
        return OrPredicate.class;
    }

    private ParseResult<OrPredicate> parseOr(Cursor cur, ParseContext ctx) {
        // extract the expression before the OR operator to avoid recursive parsing.
        var exprCur = cur.advance(cur.find(TokenType.OR));
        var lhs = ctx.parse(Predicate.class, exprCur);
        if (lhs.isError()) {
            return error(lhs);
        }

        cur.expect("Expected OR", TokenType.OR);

        var rhs = ctx.parse(Predicate.class, cur);
        if (rhs.isError()) {
            return error(rhs);
        }
        return finalize(cur, ctx, OrPredicate.of(lhs.value(), rhs.value()));
    }
}
