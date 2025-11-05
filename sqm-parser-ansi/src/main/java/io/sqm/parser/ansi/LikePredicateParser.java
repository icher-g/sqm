package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.LikePredicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class LikePredicateParser implements Parser<LikePredicate> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<LikePredicate> parse(Cursor cur, ParseContext ctx) {
        var expr = ctx.parse(Expression.class, cur);
        if (expr.isError()) {
            return error(expr);
        }

        var negated = cur.consumeIf(TokenType.NOT);
        cur.expect("Expected LIKE", TokenType.LIKE);

        var pattern = ctx.parse(Expression.class, cur);
        if (pattern.isError()) {
            return error(pattern);
        }

        Expression escape = null;
        if (cur.consumeIf(TokenType.ESCAPE)) {
            var result = ctx.parse(Expression.class, cur);
            if (result.isError()) {
                return error(result);
            }
            escape = result.value();
        }

        return finalize(cur, ctx, LikePredicate.of(expr.value(), pattern.value(), escape, negated));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<LikePredicate> targetType() {
        return LikePredicate.class;
    }
}
