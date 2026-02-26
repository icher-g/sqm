package io.sqm.parser.ansi;

import io.sqm.core.AnyAllPredicate;
import io.sqm.core.Expression;
import io.sqm.core.Quantifier;
import io.sqm.core.Query;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parser for quantified ANY/ALL predicates.
 */
public class AnyAllPredicateParser implements Parser<AnyAllPredicate>, InfixParser<Expression, AnyAllPredicate> {

    /**
     * Creates an ANY/ALL predicate parser.
     */
    public AnyAllPredicateParser() {
    }

    private final ComparisonOperatorParser operatorParser = new ComparisonOperatorParser();

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<AnyAllPredicate> parse(Cursor cur, ParseContext ctx) {
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
    public Class<AnyAllPredicate> targetType() {
        return AnyAllPredicate.class;
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
    public ParseResult<AnyAllPredicate> parse(Expression lhs, Cursor cur, ParseContext ctx) {
        var operator = operatorParser.parse(cur, ctx);

        Quantifier quantifier;
        if (cur.consumeIf(TokenType.ANY)) {
            quantifier = Quantifier.ANY;
        }
        else
            if (cur.consumeIf(TokenType.ALL)) {
                quantifier = Quantifier.ALL;
            }
            else {
                return error("Unexpected quantifier token: " + cur.peek().lexeme(), cur.fullPos());
            }

        cur.expect("Expected (", TokenType.LPAREN);

        var query = ctx.parse(Query.class, cur);
        if (query.isError()) {
            return error(query);
        }

        cur.expect("Expected )", TokenType.RPAREN);
        return ok(AnyAllPredicate.of(lhs, operator, query.value(), quantifier));
    }
}
