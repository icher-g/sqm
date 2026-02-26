package io.sqm.parser.ansi;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.core.UnaryPredicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses unary predicates.
 */
public class UnaryPredicateParser implements Parser<UnaryPredicate>, InfixParser<Expression, UnaryPredicate> {
    /**
     * Creates a unary-predicate parser.
     */
    public UnaryPredicateParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<UnaryPredicate> parse(Cursor cur, ParseContext ctx) {
        if (cur.consumeIf(TokenType.TRUE)) {
            return ok(UnaryPredicate.of(Expression.literal(true)));
        }
        if (cur.consumeIf(TokenType.FALSE)) {
            return ok(UnaryPredicate.of(Expression.literal(false)));
        }

        var column = ctx.parse(ColumnExpr.class, cur);
        if (column.isError()) {
            return error(column);
        }
        return ok(UnaryPredicate.of(column.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<UnaryPredicate> targetType() {
        return UnaryPredicate.class;
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
    public ParseResult<UnaryPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
        return ok(UnaryPredicate.of(lhs));
    }
}
