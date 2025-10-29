package io.sqm.parser.ansi;

import io.sqm.core.CrossJoin;
import io.sqm.core.Join;
import io.sqm.core.TableRef;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class CrossJoinParser implements Parser<CrossJoin> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<CrossJoin> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected CROSS", TokenType.CROSS);
        cur.expect("Expected JOIN", TokenType.JOIN);

        var table = ctx.parse(TableRef.class, cur);
        if (table.isError()) {
            return error(table);
        }
        return ok(Join.cross(table.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<CrossJoin> targetType() {
        return CrossJoin.class;
    }
}
