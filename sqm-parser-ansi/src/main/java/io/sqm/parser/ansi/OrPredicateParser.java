package io.sqm.parser.ansi;

import io.sqm.core.AndPredicate;
import io.sqm.core.OrPredicate;
import io.sqm.core.Predicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class OrPredicateParser implements Parser<Predicate> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends Predicate> parse(Cursor cur, ParseContext ctx) {
        ParseResult<? extends Predicate> lhs = ctx.parse(AndPredicate.class, cur);
        if (lhs.isError()) {
            return error(lhs);
        }

        while (cur.consumeIf(TokenType.OR)) {
            var rhs = ctx.parse(AndPredicate.class, cur);
            if (rhs.isError()) {
                return error(rhs);
            }
            lhs = ok(OrPredicate.of(lhs.value(), rhs.value()));
        }

        return lhs;
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
}
