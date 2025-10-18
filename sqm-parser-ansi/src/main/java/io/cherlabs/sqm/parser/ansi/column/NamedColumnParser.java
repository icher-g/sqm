package io.cherlabs.sqm.parser.ansi.column;

import io.cherlabs.sqm.core.NamedColumn;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.TokenType;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

public class NamedColumnParser implements Parser<NamedColumn> {

    @Override
    public ParseResult<NamedColumn> parse(Cursor cur, ParseContext ctx) {
        // 1) Try: table.name or name
        if (cur.match(TokenType.IDENT)) {
            String table = null, name = null;

            // Option A: t1.c1 [alias]
            if (cur.peek(1).type() == TokenType.DOT && cur.peek(2).type() == TokenType.IDENT) {
                table = cur.advance().lexeme();
                cur.advance(); // skip DOT
                name = cur.advance().lexeme();
            } else {
                // Option B: c1 [alias]
                if (cur.matchAny(1, TokenType.AS, TokenType.IDENT, TokenType.EOF)) {
                    name = cur.advance().lexeme();
                }
            }

            var alias = parseAlias(cur);
            return ok(new NamedColumn(name, alias, table));
        }

        return error("Unexpected tokens at the beginning of expr, expected identifier but found: " + cur.peek(), cur.fullPos());
    }

    @Override
    public Class<NamedColumn> targetType() {
        return NamedColumn.class;
    }
}
