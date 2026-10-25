package io.sqm.parser.ansi;

import io.sqm.core.CellAddress;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for CellAddress.
 */
public class CellAddressParser implements Parser<CellAddress> {
    /**
     * Creates the CellAddress parser.
     */
    public CellAddressParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellAddress> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL CellAddress is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellAddress> targetType() {
        return CellAddress.class;
    }
}
