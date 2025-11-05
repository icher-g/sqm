package io.sqm.parser.ansi;

import io.sqm.core.BetweenPredicate;
import io.sqm.core.Expression;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class BetweenPredicateParser implements Parser<BetweenPredicate> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<BetweenPredicate> parse(Cursor cur, ParseContext ctx) {
        var value = ctx.parse(Expression.class, cur);
        if (value.isError()) {
            return error(value);
        }

        cur.expect("Expect BETWEEN", TokenType.BETWEEN);
        var symmetric = cur.consumeIf(TokenType.SYMMETRIC);

        var lower = ctx.parse(Expression.class, cur);
        if (lower.isError()) {
            return error(lower);
        }

        cur.expect("Expect AND", TokenType.AND);

        var upper = ctx.parse(Expression.class, cur);
        if (upper.isError()) {
            return error(upper);
        }
        return finalize(cur, ctx, BetweenPredicate.of(value.value(), lower.value(), upper.value(), symmetric));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<BetweenPredicate> targetType() {
        return BetweenPredicate.class;
    }
}
