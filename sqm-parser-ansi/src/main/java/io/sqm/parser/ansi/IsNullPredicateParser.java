package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.IsNullPredicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class IsNullPredicateParser implements Parser<IsNullPredicate> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<IsNullPredicate> parse(Cursor cur, ParseContext ctx) {
        var value = ctx.parse(Expression.class, cur);
        if (value.isError()) {
            return error(value);
        }

        cur.expect("Expected IS", TokenType.IS);
        var negated = cur.consumeIf(TokenType.NOT);
        cur.expect("Expected NULL", TokenType.NULL);

        return finalize(cur, ctx, IsNullPredicate.of(value.value(), negated));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<IsNullPredicate> targetType() {
        return IsNullPredicate.class;
    }
}
