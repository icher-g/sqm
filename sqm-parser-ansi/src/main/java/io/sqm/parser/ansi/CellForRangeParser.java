package io.sqm.parser.ansi;

import io.sqm.core.CellFor;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit ANSI unsupported boundary for CellFor.Range; supporting dialects override it.
 */
public class CellForRangeParser implements Parser<CellFor.Range> {
    /**
     * Creates the CellFor.Range parser.
     */
    public CellForRangeParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellFor.Range> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL CellFor.Range is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellFor.Range> targetType() {
        return CellFor.Range.class;
    }
}
