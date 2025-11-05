package io.sqm.parser.ansi;

import io.sqm.core.LiteralExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class LiteralExprParser implements Parser<LiteralExpr> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<LiteralExpr> parse(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.STRING)) {
            return finalize(cur, ctx, LiteralExpr.of(cur.advance().lexeme()));
        }
        if (cur.match(TokenType.NUMBER)) {
            return finalize(cur, ctx, LiteralExpr.of(parseNumber(cur.advance().lexeme())));
        }
        if (cur.match(TokenType.NULL)) {
            cur.advance(); // skip the literal itself
            return finalize(cur, ctx, LiteralExpr.of(null));
        }
        if (cur.match(TokenType.TRUE)) {
            cur.advance(); // skip the literal itself
            return finalize(cur, ctx, LiteralExpr.of(Boolean.TRUE));
        }
        if (cur.match(TokenType.FALSE)) {
            cur.advance(); // skip the literal itself
            return finalize(cur, ctx, LiteralExpr.of(Boolean.FALSE));
        }
        return error("Unsupported literal token: " + cur.peek().lexeme(), cur.fullPos());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<LiteralExpr> targetType() {
        return LiteralExpr.class;
    }
}
