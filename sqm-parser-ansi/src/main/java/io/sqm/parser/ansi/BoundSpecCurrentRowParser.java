package io.sqm.parser.ansi;

import io.sqm.core.BoundSpec;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses {@code CURRENT ROW} frame bounds.
 */
public class BoundSpecCurrentRowParser implements Parser<BoundSpec.CurrentRow> {
    /**
     * Creates a current-row bound parser.
     */
    public BoundSpecCurrentRowParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<BoundSpec.CurrentRow> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected CURRENT", TokenType.CURRENT);
        cur.expect("Expected ROW after CURRENT", TokenType.ROW);
        return ok(BoundSpec.currentRow());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<BoundSpec.CurrentRow> targetType() {
        return BoundSpec.CurrentRow.class;
    }
}
