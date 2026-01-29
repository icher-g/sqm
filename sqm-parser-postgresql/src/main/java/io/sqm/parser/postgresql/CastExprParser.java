package io.sqm.parser.postgresql;

import io.sqm.core.CastExpr;
import io.sqm.core.Expression;
import io.sqm.core.TypeName;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.InfixParser;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class CastExprParser implements MatchableParser<CastExpr>, InfixParser<Expression, CastExpr> {

    private final MatchableParser<CastExpr> ansiParser = new io.sqm.parser.ansi.CastExprParser();

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends CastExpr> parse(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.CAST)) {
            return ansiParser.parse(cur, ctx);
        }

        var left = ctx.parse(Expression.class, cur);
        if (left.isError()) {
            return error(left);
        }

        return parse(left.value(), cur, ctx);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends CastExpr> targetType() {
        return CastExpr.class;
    }

    /**
     * Performs a look-ahead test to determine whether this parser is applicable
     * at the current cursor position.
     * <p>
     * Implementations must <strong>not</strong> advance the cursor or modify
     * the {@link ParseContext}. Their sole responsibility is to inspect the
     * upcoming tokens and decide if this parser is responsible for them.
     *
     * @param cur the cursor pointing at the current token
     * @param ctx the parsing context providing configuration and utilities
     * @return {@code true} if this parser should be used to parse the upcoming
     * input, {@code false} otherwise
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return ansiParser.match(cur, ctx) || cur.match(TokenType.DOUBLE_COLON);
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
    public ParseResult<CastExpr> parse(Expression lhs, Cursor cur, ParseContext ctx) {
        cur.expect("Expected ::", TokenType.DOUBLE_COLON);
        ParseResult<? extends TypeName> rhs = ctx.parse(TypeName.class, cur);
        if (rhs.isError()) {
            return error(rhs);
        }
        return ok(CastExpr.of(lhs, rhs.value()));
    }
}
