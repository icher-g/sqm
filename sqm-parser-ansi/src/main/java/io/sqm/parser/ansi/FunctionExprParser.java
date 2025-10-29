package io.sqm.parser.ansi;

import io.sqm.core.FunctionExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

public class FunctionExprParser implements Parser<FunctionExpr> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<FunctionExpr> parse(Cursor cur, ParseContext ctx) {
        var t = cur.expect("Expected function name", TokenType.IDENT);

        StringBuilder name = new StringBuilder(t.lexeme());
        while (cur.consumeIf(TokenType.DOT) && cur.match(TokenType.IDENT, 1)) {
            name.append('.').append(cur.advance().lexeme());
        }

        // '('
        cur.expect("Expected '(' after function name", TokenType.LPAREN);

        final Boolean distinct = cur.consumeIf(TokenType.DISTINCT) ? true : null;
        final List<FunctionExpr.Arg> args = new ArrayList<>();

        do {
            var vr = ctx.parse(FunctionExpr.Arg.class, cur);
            if (vr.isError()) {
                return error(vr);
            }
            args.add(vr.value());
        } while (cur.consumeIf(TokenType.COMMA));

        // ')'
        cur.expect("Expected ')' to close function", TokenType.RPAREN);

        return ok(FunctionExpr.of(name.toString(), distinct, args));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<FunctionExpr> targetType() {
        return FunctionExpr.class;
    }
}
