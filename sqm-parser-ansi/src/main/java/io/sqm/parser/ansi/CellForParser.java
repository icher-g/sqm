package io.sqm.parser.ansi;

import io.sqm.core.CellFor;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit ANSI unsupported boundary for CellFor; supporting dialects override it.
 */
public class CellForParser implements Parser<CellFor> {
    /**
     * Creates the CellFor parser.
     */
    public CellForParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellFor> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL CellFor is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellFor> targetType() {
        return CellFor.class;
    }
}
