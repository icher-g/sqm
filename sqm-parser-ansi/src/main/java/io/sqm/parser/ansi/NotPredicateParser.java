package io.sqm.parser.ansi;

import io.sqm.core.NotPredicate;
import io.sqm.core.Predicate;
import io.sqm.parser.AtomicPredicateParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class NotPredicateParser implements Parser<Predicate> {

    private final AtomicPredicateParser atomicPredicateParser;

    public NotPredicateParser(AtomicPredicateParser atomicPredicateParser) {
        this.atomicPredicateParser = atomicPredicateParser;
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends Predicate> parse(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.NOT) && cur.match(TokenType.LPAREN, 1)) {
            cur.expect("Expected NOT", TokenType.NOT);
            cur.expect("Expected (", TokenType.LPAREN);

            var result = ctx.parse(Predicate.class, cur);
            if (result.isError()) {
                return error(result);
            }

            cur.expect("Expected )", TokenType.RPAREN);
            return ok(NotPredicate.of(result.value()));
        }
        return atomicPredicateParser.parse(cur, ctx);
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
