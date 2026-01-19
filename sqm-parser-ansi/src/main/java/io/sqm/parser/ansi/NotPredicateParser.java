package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.AtomicPredicateParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parser responsible for handling the {@code NOT} prefix in predicate contexts.
 *
 * <p>This parser does <strong>not</strong> always return a {@link NotPredicate}.
 * Depending on the token sequence following {@code NOT}, it may delegate to
 * other predicate parsers or normalize the parsed structure.</p>
 *
 * <p>Supported forms include:</p>
 * <ul>
 *   <li>{@code NOT EXISTS (...)} – delegated directly to {@link ExistsPredicate} parsing</li>
 *   <li>{@code NOT (predicate)} – wrapped as {@link NotPredicate}</li>
 *   <li>{@code NOT expression} – normalized to {@code NotPredicate(UnaryPredicate(expression))}</li>
 * </ul>
 *
 * <p>If the {@code NOT} token is not present, parsing is delegated to
 * {@link AtomicPredicateParser}.</p>
 *
 * <p>This design allows the parser to preserve canonical predicate forms
 * (e.g. {@code NOT EXISTS} instead of {@code NOT (EXISTS ...)}) while still
 * supporting generic negation of predicates and boolean expressions.</p>
 */
public class NotPredicateParser implements Parser<Predicate> {

    private final AtomicPredicateParser atomicPredicateParser;

    public NotPredicateParser(AtomicPredicateParser atomicPredicateParser) {
        this.atomicPredicateParser = atomicPredicateParser;
    }

    /**
     * Parses a predicate with an optional {@code NOT} prefix.
     *
     * <p>Depending on the parsed structure, the returned predicate may be
     * a {@link NotPredicate} or another predicate type produced via delegation.</p>
     *
     * @param cur a cursor over the token stream
     * @param ctx a parser context providing access to other parsers
     * @return a parsing result containing a {@link Predicate}
     */
    @Override
    public ParseResult<? extends Predicate> parse(Cursor cur, ParseContext ctx) {
        if (cur.consumeIf(TokenType.NOT)) {
            // NOT EXISTS
            if (cur.match(TokenType.EXISTS)) {
                return ctx.parse(ExistsPredicate.class, cur);
            }

            // NOT ( predicate )
            if (cur.match(TokenType.LPAREN)) {
                cur.expect("Expected (", TokenType.LPAREN);

                var result = ctx.parse(Predicate.class, cur);
                if (result.isError()) {
                    return error(result);
                }

                cur.expect("Expected )", TokenType.RPAREN);
                return ok(NotPredicate.of(result.value()));
            }

            // NOT expression
            var result = ctx.parse(Expression.class, cur);
            if (result.isError()) {
                return error(result);
            }
            return ok(NotPredicate.of(UnaryPredicate.of(result.value())));
        }
        return atomicPredicateParser.parse(cur, ctx);
    }

    /**
     * Returns {@link NotPredicate} as the primary target type of this parser.
     *
     * <p>Note that this parser may return other {@link Predicate} implementations
     * as a result of delegation or normalization.</p>
     *
     * @return {@link NotPredicate}.class
     */
    @Override
    public Class<NotPredicate> targetType() {
        return NotPredicate.class;
    }
}

