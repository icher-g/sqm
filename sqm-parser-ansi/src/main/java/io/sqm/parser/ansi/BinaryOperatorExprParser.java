package io.sqm.parser.ansi;

import io.sqm.core.ArithmeticExpr;
import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.Expression;
import io.sqm.core.OperatorName;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parser for generic binary-operator expressions.
 */
public class BinaryOperatorExprParser implements Parser<Expression>, InfixParser<Expression, BinaryOperatorExpr> {
    /**
     * Creates a binary-operator expression parser.
     */
    public BinaryOperatorExprParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
        ParseResult<? extends Expression> lhs = ctx.parse(ArithmeticExpr.class, cur);
        if (lhs.isError()) {
            return lhs;
        }

        while (ctx.operatorPolicy().isGenericBinaryOperator(cur.peek())) {
            if (!ctx.capabilities().supports(SqlFeature.CUSTOM_OPERATOR)) {
                return error("Dialect does not support custom operators", cur.fullPos());
            }
            lhs = parse(lhs.value(), cur, ctx);
        }

        return lhs;
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends BinaryOperatorExpr> targetType() {
        return BinaryOperatorExpr.class;
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
    public ParseResult<BinaryOperatorExpr> parse(Expression lhs, Cursor cur, ParseContext ctx) {
        if (!ctx.capabilities().supports(SqlFeature.CUSTOM_OPERATOR)) {
            return error("Dialect does not support custom operators", cur.fullPos());
        }
        var token = cur.expect("Expected operator", TokenType.OPERATOR, TokenType.QMARK);
        var rhs = ctx.parse(ArithmeticExpr.class, cur);
        if (rhs.isError()) {
            return error(rhs);
        }
        return ok(BinaryOperatorExpr.of(lhs, OperatorName.of(token.lexeme()), rhs.value()));
    }
}
