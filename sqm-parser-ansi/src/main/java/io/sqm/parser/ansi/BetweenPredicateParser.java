package io.sqm.parser.ansi;

import io.sqm.core.BetweenPredicate;
import io.sqm.core.Expression;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class BetweenPredicateParser implements Parser<BetweenPredicate>, InfixParser<Expression, BetweenPredicate> {
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
        return parse(value.value(), cur, ctx);
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

    /**
     * Parses a binary operator occurrence where the left-hand side operand
     * has already been parsed.
     *
     * <p>The cursor is positioned at the operator token when this method
     * is invoked. Implementations are responsible for consuming the operator
     * token, parsing the right-hand side operand, and constructing the
     * resulting node.</p>
     *
     * @param lhs the already parsed left-hand operand
     * @param cur the cursor positioned at the operator token
     * @param ctx the parse context
     * @return the parsing result representing {@code lhs <op> rhs}
     */
    @Override
    public ParseResult<BetweenPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
        var negated = cur.consumeIf(TokenType.NOT);

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
        return ok(BetweenPredicate.of(lhs, lower.value(), upper.value(), symmetric, negated));
    }
}
