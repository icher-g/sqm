package io.sqm.parser.ansi;

import io.sqm.core.Table;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class TableParser implements MatchableParser<Table> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<Table> parse(Cursor cur, ParseContext ctx) {
        Table.Inheritance inheritance = Table.Inheritance.DEFAULT;

        if (cur.consumeIf(TokenType.ONLY)) {
            if (!ctx.capabilities().supports(SqlFeature.TABLE_INHERITANCE_ONLY)) {
                return error("ONLY is not supported by this dialect", cur.fullPos());
            }
            inheritance = Table.Inheritance.ONLY;
        }
        // first identifier
        var t = cur.expect("Expected identifier", TokenType.IDENT);

        List<String> parts = new ArrayList<>();
        parts.add(t.lexeme());

        // dot-separated identifiers
        while (cur.consumeIf(TokenType.DOT)) {
            t = cur.expect("Expected identifier after '.'", TokenType.IDENT);
            parts.add(t.lexeme());
        }

        if (cur.match(TokenType.OPERATOR) && "*".equals(cur.peek().lexeme())) {
            if (!ctx.capabilities().supports(SqlFeature.TABLE_INHERITANCE_DESCENDANTS)) {
                return error("Table inheritance '*' is not supported by this dialect", cur.fullPos());
            }
            if (inheritance == Table.Inheritance.ONLY) {
                return error("ONLY cannot be combined with inheritance '*'", cur.fullPos());
            }
            cur.advance();
            inheritance = Table.Inheritance.INCLUDE_DESCENDANTS;
        }

        // optional alias: AS identifier | bare identifier
        String alias = parseAlias(cur);

        // Map parts → schema + name
        String name = parts.getLast();
        String schema = parts.size() > 1 ? String.join(".", parts.subList(0, parts.size() - 1)) : null;

        return ok(Table.of(schema, name, alias, inheritance));
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

    /**
     * Performs a look-ahead test to determine whether this parser is applicable
     * at the current cursor position.
     * <p>
     * Implementations must <strong>not</strong> advance the cursor or modify
     * the {@link ParseContext}. Their sole responsibility is to inspect the
     * upcoming tokens and decide if this parser is responsible for them.
     *
     * @param cur the cursor pointing at the current token
     * @param ctx the parsing context providing configuration and utilities
     * @return {@code true} if this parser should be used to parse the upcoming
     * input, {@code false} otherwise
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.ONLY)) {
            return cur.match(TokenType.IDENT, 1);
        }
        if (cur.match(TokenType.IDENT)) {
            int i = 1;
            while (cur.match(TokenType.DOT, i)) {
                if (!cur.match(TokenType.IDENT, i + 1)) {
                    return false;
                }
                i += 2;
            }
            return true;
        }
        return false;
    }
}
