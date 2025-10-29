package io.sqm.parser.ansi;

import io.sqm.core.ComparisonPredicate;
import io.sqm.core.Expression;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class ComparisonPredicateParser implements Parser<ComparisonPredicate> {

    private final ComparisonOperatorParser operatorParser = new ComparisonOperatorParser();

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<ComparisonPredicate> parse(Cursor cur, ParseContext ctx) {
        var lhs = ctx.parse(Expression.class, cur);
        if (lhs.isError()) {
            return error(lhs);
        }

        var operator = operatorParser.parse(cur, ctx);

        var rhs = ctx.parse(Expression.class, cur);
        if (rhs.isError()) {
            return error(rhs);
        }
        return ok(ComparisonPredicate.of(lhs.value(), operator, rhs.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ComparisonPredicate> targetType() {
        return ComparisonPredicate.class;
    }
}
