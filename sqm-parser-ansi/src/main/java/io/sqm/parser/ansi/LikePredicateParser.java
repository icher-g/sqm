package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.LikePredicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class LikePredicateParser implements Parser<LikePredicate>, InfixParser<Expression, LikePredicate> {
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
        return parse(expr.value(), cur, ctx);
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
    public ParseResult<LikePredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
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

        return ok(LikePredicate.of(lhs, pattern.value(), escape, negated));
    }
}
