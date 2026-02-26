package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.IsNullPredicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses {@code IS [NOT] NULL} predicates.
 */
public class IsNullPredicateParser implements Parser<IsNullPredicate>, InfixParser<Expression, IsNullPredicate> {
    /**
     * Creates an IS NULL predicate parser.
     */
    public IsNullPredicateParser() {
    }

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
        return parse(value.value(), cur, ctx);
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
    public ParseResult<IsNullPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
        cur.expect("Expected IS", TokenType.IS);
        var negated = cur.consumeIf(TokenType.NOT);
        cur.expect("Expected NULL", TokenType.NULL);

        return ok(IsNullPredicate.of(lhs, negated));
    }
}
