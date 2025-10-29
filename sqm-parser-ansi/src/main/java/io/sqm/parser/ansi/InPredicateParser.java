package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.InPredicate;
import io.sqm.core.ValueSet;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class InPredicateParser implements Parser<InPredicate> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<InPredicate> parse(Cursor cur, ParseContext ctx) {
        var expr = ctx.parse(Expression.class, cur);
        if (expr.isError()) {
            return error(expr);
        }

        var negated = cur.consumeIf(TokenType.NOT);
        cur.expect("Expected IN", TokenType.IN);

        var values = ctx.parse(ValueSet.class, cur);
        if (values.isError()) {
            return error(values);
        }
        return ok(InPredicate.of(expr.value(), values.value(), negated));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<InPredicate> targetType() {
        return InPredicate.class;
    }
}
