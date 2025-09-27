package io.cherlabs.sqlmodel.parser;

import io.cherlabs.sqlmodel.core.NamedTable;
import io.cherlabs.sqlmodel.core.Table;
import io.cherlabs.sqlmodel.parser.core.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses table specs like:
 * products
 * sales.products p
 * sales.products AS p
 * server.db.schema.table t  (schema becomes "server.db.schema")
 */
public final class TableSpecParser implements SpecParser<Table> {

    @Override
    public Class<Table> targetType() {
        return Table.class;
    }

    @Override
    public ParseResult<Table> parse(Cursor cur) {
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

        // must be EOF now
        cur.expect("Unexpected tokens after table: " + cur.peek(), TokenType.EOF);

        // Map parts → schema + name
        String name = parts.get(parts.size() - 1);
        String schema = parts.size() > 1 ? String.join(".", parts.subList(0, parts.size() - 1)) : null;

        return ParseResult.ok(new NamedTable(name, alias, schema));
    }
}
