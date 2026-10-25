package io.sqm.parser.ansi;

import io.sqm.core.CellSelector;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for CellSelector.Value.
 */
public class CellSelectorValueParser implements Parser<CellSelector.Value> {
    /**
     * Creates the CellSelector.Value parser.
     */
    public CellSelectorValueParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellSelector.Value> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL CellSelector.Value is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellSelector.Value> targetType() {
        return CellSelector.Value.class;
    }
}
