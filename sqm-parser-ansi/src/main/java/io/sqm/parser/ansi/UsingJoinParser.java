package io.sqm.parser.ansi;

import io.sqm.core.Join;
import io.sqm.core.TableRef;
import io.sqm.core.UsingJoin;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

public class UsingJoinParser implements Parser<UsingJoin> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<UsingJoin> parse(Cursor cur, ParseContext ctx) {
        // JOIN customers USING (customer_id, region_id);
        cur.expect("Expected JOIN", TokenType.JOIN);

        var table = ctx.parse(TableRef.class, cur);
        if (table.isError()) {
            return error(table);
        }

        cur.expect("Expected USING", TokenType.USING);
        cur.expect("Expected (", TokenType.LPAREN);

        final List<String> columns = new ArrayList<>();
        do {
            var column = cur.advance().lexeme();
            columns.add(column);
        } while (cur.consumeIf(TokenType.COMMA));

        cur.expect("Expected )", TokenType.RPAREN);
        return ok(Join.using(table.value(), columns));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<UsingJoin> targetType() {
        return UsingJoin.class;
    }
}
