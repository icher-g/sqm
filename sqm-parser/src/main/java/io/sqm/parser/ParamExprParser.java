package io.sqm.parser;

import io.sqm.core.ParamExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class ParamExprParser implements Parser<ParamExpr> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<ParamExpr> parse(Cursor cur, ParseContext ctx) {
        if (cur.consumeIf(TokenType.PARAM_QMARK)) {
            return finalize(cur, ctx, ParamExpr.anonymous());
        }
        if (cur.match(TokenType.PARAM_NAMED)) {
            return finalize(cur, ctx, ParamExpr.named(cur.advance().lexeme()));
        }
        if (cur.match(TokenType.PARAM_POS)) {
            return finalize(cur, ctx, ParamExpr.ordinal(Integer.parseInt(cur.advance().lexeme())));
        }
        throw new IllegalArgumentException("The specified parameter: " + cur.peek().lexeme() + " is not supported.");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ParamExpr> targetType() {
        return ParamExpr.class;
    }
}
