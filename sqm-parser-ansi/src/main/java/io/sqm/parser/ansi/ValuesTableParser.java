package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class ValuesTableParser implements MatchableParser<ValuesTable> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<ValuesTable> parse(Cursor cur, ParseContext ctx) {
        // (VALUES (1, 'A'), (2, 'B')) AS v(id, name)

        cur.expect("Expected (", TokenType.LPAREN);
        cur.expect("Expected VALUES", TokenType.VALUES);

        List<RowExpr> items = new ArrayList<>();
        do {
            var rowExpr = ctx.parse(RowExpr.class, cur);
            if (rowExpr.isError()) {
                return error(rowExpr);
            }
            items.add(rowExpr.value());
        }
        while (cur.consumeIf(TokenType.COMMA));

        RowValues rows = items.size() == 1 ? items.getFirst() : RowListExpr.of(items);

        cur.expect("Expected )", TokenType.RPAREN);

        var aliases = parseColumnAliasIdentifiers(cur);
        return ok(TableRef.values(rows).as(aliases.first()).columnAliases(aliases.second()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ValuesTable> targetType() {
        return ValuesTable.class;
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
        return cur.match(TokenType.LPAREN) && cur.match(TokenType.VALUES, 1);
    }
}
