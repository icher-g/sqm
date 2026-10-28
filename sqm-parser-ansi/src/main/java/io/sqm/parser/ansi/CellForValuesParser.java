package io.sqm.parser.ansi;

import io.sqm.core.CellFor;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit ANSI unsupported boundary for CellFor.Values; supporting dialects override it.
 */
public class CellForValuesParser implements Parser<CellFor.Values> {
    /**
     * Creates the CellFor.Values parser.
     */
    public CellForValuesParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellFor.Values> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL CellFor.Values is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellFor.Values> targetType() {
        return CellFor.Values.class;
    }
}
