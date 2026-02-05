package io.sqm.parser.postgresql;

import io.sqm.core.ArithmeticExpr;
import io.sqm.core.BinaryOperatorExpr;
import io.sqm.core.Expression;
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

        while (true) {
            if (ctx.operatorPolicy().isGenericBinaryOperator(cur.peek())) {
                if (!ctx.capabilities().supports(SqlFeature.CUSTOM_OPERATOR)) {
                    return error("Dialect does not support custom operators", cur.fullPos());
                }
                lhs = parse(lhs.value(), cur, ctx);
                if (lhs.isError()) {
                    return lhs;
                }
                continue;
            }
            if (isOperatorSyntax(cur)) {
                if (!ctx.capabilities().supports(SqlFeature.CUSTOM_OPERATOR)) {
                    return error("Dialect does not support custom operators", cur.fullPos());
                }
                lhs = parseOperatorSyntax(lhs.value(), cur, ctx);
                if (lhs.isError()) {
                    return lhs;
                }
                continue;
            }
            break;
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
        return ok(BinaryOperatorExpr.of(lhs, token.lexeme(), rhs.value()));
    }

    private boolean isOperatorSyntax(Cursor cur) {
        return cur.match(TokenType.IDENT, "OPERATOR") && cur.match(TokenType.LPAREN, 1);
    }

    private ParseResult<BinaryOperatorExpr> parseOperatorSyntax(Expression lhs, Cursor cur, ParseContext ctx) {
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

        var rhs = ctx.parse(ArithmeticExpr.class, cur);
        if (rhs.isError()) {
            return error(rhs);
        }
        return ok(BinaryOperatorExpr.of(lhs, operator, rhs.value()));
    }
}
