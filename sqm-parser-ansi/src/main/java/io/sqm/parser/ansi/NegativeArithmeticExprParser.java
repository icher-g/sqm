package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.NegativeArithmeticExpr;
import io.sqm.core.PowerArithmeticExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.core.OperatorTokens.isMinus;
import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses unary negative arithmetic expressions.
 */
public class NegativeArithmeticExprParser implements Parser<NegativeArithmeticExpr> {
    /**
     * Creates a negative-expression parser.
     */
    public NegativeArithmeticExprParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<NegativeArithmeticExpr> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected - before expression", t -> isMinus(t));
        ParseResult<? extends Expression> result;
        var enclosed = cur.consumeIf(TokenType.LPAREN);
        if (enclosed) {
            result = ctx.parse(Expression.class, cur);
        }
        else {
            result = ctx.parse(PowerArithmeticExpr.class, cur);
        }
        if (result.isError()) {
            return error(result);
        }
        if (enclosed) cur.expect("Expected )", TokenType.RPAREN);
        return ok(NegativeArithmeticExpr.of(result.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<NegativeArithmeticExpr> targetType() {
        return NegativeArithmeticExpr.class;
    }
}
