package io.sqm.parser.ansi;

import io.sqm.core.CastExpr;
import io.sqm.core.Expression;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class CastExprParser implements MatchableParser<CastExpr> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends CastExpr> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected CASE but found: " + cur.peek(), TokenType.CAST);
        cur.expect("Expected (", TokenType.LPAREN);
        var operand = ctx.parse(Expression.class, cur);
        if (operand.isError()) {
            return error(operand);
        }
        cur.expect("Expected AS", TokenType.AS);
        var type = cur.expect("Expected identifier", TokenType.IDENT);
        cur.expect("Expected )", TokenType.RPAREN);
        return ok(CastExpr.of(operand.value(), type.lexeme()));
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
        return cur.match(TokenType.CAST);
    }
}
