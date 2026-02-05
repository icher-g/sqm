package io.sqm.parser.postgresql;

import io.sqm.core.ArithmeticExpr;
import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.Expression;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.*;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * PostgreSQL-specific binary operator parser.
 * <p>
 * Adds support for {@code OPERATOR(schema.op)} infix syntax while preserving
 * generic operator parsing behavior.
 */
public class BinaryOperatorExprParser implements Parser<Expression>, InfixParser<Expression, BinaryOperatorExpr> {

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

        if (peekOperator(cur, ctx) == null) {
            return lhs;
        }

        return parse(lhs.value(), cur, ctx);
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

        if (peekOperator(cur, ctx) == null) {
            return error("Expected operator", cur.fullPos());
        }

        var result = parseCustomOperators(lhs, cur, ctx, OperatorPrecedence.CUSTOM_LOW);
        if (result.isError()) {
            return error(result);
        }

        if (!(result.value() instanceof BinaryOperatorExpr expr)) {
            return error("Expected binary operator expression", cur.fullPos());
        }
        return ok(expr);
    }

    private boolean isOperatorSyntax(Cursor cur) {
        return cur.match(TokenType.IDENT, "OPERATOR") && cur.match(TokenType.LPAREN, 1);
    }

    private ParseResult<? extends Expression> parseCustomOperators(Expression lhs, Cursor cur, ParseContext ctx, OperatorPrecedence minPrecedence) {
        Expression left = lhs;

        while (true) {
            var operatorPrecedence = peekOperator(cur, ctx);
            if (operatorPrecedence == null || operatorPrecedence.level() < minPrecedence.level()) {
                break;
            }

            var operator = consumeOperator(cur);
            if (operator.isError()) {
                return error(operator);
            }

            ParseResult<? extends Expression> rhs = ctx.parse(ArithmeticExpr.class, cur);
            if (rhs.isError()) {
                return error(rhs);
            }

            var lookahead = peekOperator(cur, ctx);

            while (lookahead != null && lookahead.level() > operatorPrecedence.level()) {
                var rhsResult = parseCustomOperators(rhs.value(), cur, ctx, lookahead);
                if (rhsResult.isError()) {
                    return rhsResult;
                }
                rhs = rhsResult;
                lookahead = peekOperator(cur, ctx);
            }

            left = BinaryOperatorExpr.of(left, operator.value(), rhs.value());
        }

        return ok(left);
    }

    private OperatorPrecedence peekOperator(Cursor cur, ParseContext ctx) {
        if (ctx.operatorPolicy().isGenericBinaryOperator(cur.peek()) || isOperatorSyntax(cur)) {
            return ctx.operatorPolicy().customOperatorPrecedence(cur.peek().lexeme());
        }
        return null;
    }

    private ParseResult<String> consumeOperator(Cursor cur) {
        if (isOperatorSyntax(cur)) {
            return consumeOperatorSyntax(cur);
        }

        var token = cur.expect("Expected operator", TokenType.OPERATOR, TokenType.QMARK);
        return ok(token.lexeme());
    }

    private ParseResult<String> consumeOperatorSyntax(Cursor cur) {
        cur.expect("Expected OPERATOR", t -> t.type() == TokenType.IDENT && "OPERATOR".equalsIgnoreCase(t.lexeme()));
        cur.expect("Expected '(' after OPERATOR", TokenType.LPAREN);

        String schema = null;
        if (cur.match(TokenType.IDENT) && cur.match(TokenType.DOT, 1)) {
            schema = cur.advance().lexeme();
            cur.advance();
        }

        var opToken = cur.expect("Expected operator name in OPERATOR()", TokenType.OPERATOR, TokenType.QMARK);
        cur.expect("Expected ')' after OPERATOR()", TokenType.RPAREN);

        var operator = schema == null
            ? "OPERATOR(" + opToken.lexeme() + ")"
            : "OPERATOR(" + schema + "." + opToken.lexeme() + ")";

        return ok(operator);
    }
}
