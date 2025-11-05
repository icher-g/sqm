package io.sqm.parser.ansi;

import io.sqm.core.Table;
import io.sqm.core.TableRef;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

public class TableParser implements Parser<Table> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<Table> parse(Cursor cur, ParseContext ctx) {
        // first identifier
        var t = cur.expect("Expected identifier", TokenType.IDENT);

        List<String> parts = new ArrayList<>();
        parts.add(t.lexeme());

        // dot-separated identifiers
        while (cur.consumeIf(TokenType.DOT)) {
            t = cur.expect("Expected identifier after '.'", TokenType.IDENT);
            parts.add(t.lexeme());
        }

        // optional alias: AS identifier | bare identifier
        String alias = parseAlias(cur);

        // Map parts → schema + name
        String name = parts.get(parts.size() - 1);
        String schema = parts.size() > 1 ? String.join(".", parts.subList(0, parts.size() - 1)) : null;

        return finalize(cur, ctx, TableRef.table(schema, name).as(alias));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<Table> targetType() {
        return Table.class;
    }
}
