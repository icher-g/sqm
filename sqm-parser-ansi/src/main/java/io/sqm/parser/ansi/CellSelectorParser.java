package io.sqm.parser.ansi;

import io.sqm.core.CellSelector;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for CellSelector.
 */
public class CellSelectorParser implements Parser<CellSelector> {
    /**
     * Creates the CellSelector parser.
     */
    public CellSelectorParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellSelector> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL CellSelector is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellSelector> targetType() {
        return CellSelector.class;
    }
}
