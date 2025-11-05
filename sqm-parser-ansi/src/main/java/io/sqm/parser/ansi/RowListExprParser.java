package io.sqm.parser.ansi;

import io.sqm.core.RowExpr;
import io.sqm.core.RowListExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

public class RowListExprParser implements Parser<RowListExpr> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<RowListExpr> parse(Cursor cur, ParseContext ctx) {
        final List<RowExpr> rows = new ArrayList<>();
        cur.expect("Expected (", TokenType.LPAREN);

        do {
            var row = ctx.parse(RowExpr.class, cur);
            if (row.isError()) {
                return error(row);
            }
            rows.add(row.value());

        } while (cur.consumeIf(TokenType.COMMA));

        cur.expect("Expected )", TokenType.RPAREN);
        return finalize(cur, ctx, RowListExpr.of(rows));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<RowListExpr> targetType() {
        return RowListExpr.class;
    }
}
