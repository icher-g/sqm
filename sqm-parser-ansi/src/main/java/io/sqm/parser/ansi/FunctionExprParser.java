package io.sqm.parser.ansi;

import io.sqm.core.FunctionExpr;
import io.sqm.core.OrderBy;
import io.sqm.core.OverSpec;
import io.sqm.core.Predicate;
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

        if (!cur.match(TokenType.RPAREN)) {
            do {
                var vr = ctx.parse(FunctionExpr.Arg.class, cur);
                if (vr.isError()) {
                    return error(vr);
                }
                args.add(vr.value());
            } while (cur.consumeIf(TokenType.COMMA));
        }

        // ')'
        cur.expect("Expected ')' to close function", TokenType.RPAREN);

        OrderBy withinGroup = null;
        if (cur.consumeIf(TokenType.WITHIN)) {
            cur.expect("Expected GROUP after WITHIN", TokenType.GROUP);
            cur.expect("Expected '(' after WITHIN GROUP", TokenType.LPAREN);
            var obr = ctx.parse(OrderBy.class, cur);
            if (obr.isError()) {
                return error(obr);
            }
            cur.expect("Expected ')' to close statement", TokenType.RPAREN);
            withinGroup = obr.value();
        }

        Predicate filter = null;
        if (cur.consumeIf(TokenType.FILTER)) {
            cur.expect("Expected '(' after FILTER", TokenType.LPAREN);
            cur.expect("Expected WHERE in a FILTER", TokenType.WHERE);
            var pr = ctx.parse(Predicate.class, cur);
            if (pr.isError()) {
                return error(pr);
            }
            cur.expect("Expected ')' to close statement", TokenType.RPAREN);
            filter = pr.value();
        }

        OverSpec over = null;
        if (cur.match(TokenType.OVER)) {
            var or = ctx.parse(OverSpec.class, cur);
            if (or.isError()) {
                return error(or);
            }
            over = or.value();
        }

        return finalize(cur, ctx, FunctionExpr.of(name.toString(), args, distinct, withinGroup, filter, over));
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
