package io.cherlabs.sqm.parser;

import io.cherlabs.sqm.core.NamedTable;
import io.cherlabs.sqm.core.Table;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses table specs like:
 * products
 * sales.products p
 * sales.products AS p
 * server.db.schema.table t  (schema becomes "server.db.schema")
 */
public final class TableParser implements Parser<Table> {

    /**
     * Gets the {@link Table} type.
     *
     * @return {@link Table} type.
     */
    @Override
    public Class<Table> targetType() {
        return Table.class;
    }

    /**
     * Parses the table specification.
     *
     * @param cur the {@link Cursor} class containing the tokens.
     * @return a parser result.
     */
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
