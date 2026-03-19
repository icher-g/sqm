package io.sqm.parser.mysql;

import io.sqm.core.ConcatExpr;
import io.sqm.core.Expression;
import io.sqm.parser.ParserAdapter;
import io.sqm.parser.ansi.BinaryOperatorExprParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses MySQL string concatenation expressions using {@code CONCAT(...)}.
 */
public class ConcatExprParser implements Parser<Expression> {

    /**
     * The adapter is used to avoid casting issues when the returned expression
     * is evaluated. The ParseResult returned by BinaryOperatorExprParser is expected
     * to contain BinaryOperatorExpr but the actual value can be different.
     */
    private final Parser<Expression> binaryParser = ParserAdapter.widen(Expression.class, new BinaryOperatorExprParser());

    /**
     * Creates a MySQL concatenation-expression parser.
     */
    public ConcatExprParser() {
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
        if (!looksLikeConcat(cur)) {
            return ctx.parse(binaryParser, cur);
        }

        cur.expect("Expected CONCAT", t -> t.type() == TokenType.IDENT && "CONCAT".equalsIgnoreCase(t.lexeme()));
        cur.expect("Expected '(' after CONCAT", TokenType.LPAREN);

        if (cur.match(TokenType.RPAREN)) {
            return error("CONCAT requires at least one argument", cur.fullPos());
        }

        var args = parseItems(Expression.class, cur, ctx);
        if (args.isError()) {
            return error(args);
        }

        cur.expect("Expected ')' to close CONCAT", TokenType.RPAREN);
        return ok(ConcatExpr.of(args.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ConcatExpr> targetType() {
        return ConcatExpr.class;
    }

    private boolean looksLikeConcat(Cursor cur) {
        return cur.match(TokenType.IDENT)
            && "CONCAT".equalsIgnoreCase(cur.peek().lexeme())
            && cur.match(TokenType.LPAREN, 1);
    }
}
