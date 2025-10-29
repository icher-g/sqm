package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.RowExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

public class RowExprParser implements Parser<RowExpr> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<RowExpr> parse(Cursor cur, ParseContext ctx) {
        final List<Expression> list = new ArrayList<>();
        cur.expect("Expected (", TokenType.LPAREN);

        do {
            var vr = ctx.parse(Expression.class, cur);
            if (vr.isError()) {
                return error(vr);
            }
            list.add(vr.value());
        } while (cur.consumeIf(TokenType.COMMA));

        cur.expect("Expected )", TokenType.RPAREN);
        return ok(RowExpr.of(list));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<RowExpr> targetType() {
        return RowExpr.class;
    }
}
