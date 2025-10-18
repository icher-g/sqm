package io.cherlabs.sqm.parser.ansi.table;

import io.cherlabs.sqm.core.NamedTable;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.TokenType;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

public class NamedTableParser implements Parser<NamedTable> {
    @Override
    public ParseResult<NamedTable> parse(Cursor cur, ParseContext ctx) {
        // first identifier
        var t = cur.expect("Expected identifier for table name", TokenType.IDENT);

        List<String> parts = new ArrayList<>();
        parts.add(t.lexeme());

        // dot-separated identifiers
        while (cur.consumeIf(TokenType.DOT)) {
            t = cur.expect("Expected identifier after '.'", TokenType.IDENT);
            parts.add(t.lexeme());
        }

        // optional alias: AS identifier | bare identifier
        String alias = null;
        if (cur.consumeIf(TokenType.AS)) {
            t = cur.expect("Expected alias after AS", TokenType.IDENT);
            alias = t.lexeme();
        } else if (cur.match(TokenType.IDENT)) {
            // bare alias
            alias = cur.advance().lexeme();
        }

        // Map parts → schema + name
        String name = parts.get(parts.size() - 1);
        String schema = parts.size() > 1 ? String.join(".", parts.subList(0, parts.size() - 1)) : null;

        return ok(new NamedTable(name, alias, schema));
    }

    @Override
    public Class<NamedTable> targetType() {
        return NamedTable.class;
    }
}
