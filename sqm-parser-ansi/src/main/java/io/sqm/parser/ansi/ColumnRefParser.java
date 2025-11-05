package io.sqm.parser.ansi;

import io.sqm.core.ColumnExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class ColumnRefParser implements Parser<ColumnExpr> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<ColumnExpr> parse(Cursor cur, ParseContext ctx) {
        var t = cur.expect("Expected identifier", TokenType.IDENT);
        String table = null, name = t.lexeme();

        // t1.c1
        if (cur.consumeIf(TokenType.DOT) && cur.match(TokenType.IDENT)) {
            table = name;
            name = cur.advance().lexeme();
        }
        return finalize(cur, ctx, ColumnExpr.of(table, name));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ColumnExpr> targetType() {
        return ColumnExpr.class;
    }
}
