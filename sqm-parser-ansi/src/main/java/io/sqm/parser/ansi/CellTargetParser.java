package io.sqm.parser.ansi;

import io.sqm.core.CellTarget;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for CellTarget.
 */
public class CellTargetParser implements Parser<CellTarget> {
    /**
     * Creates the CellTarget parser.
     */
    public CellTargetParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellTarget> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL CellTarget is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellTarget> targetType() {
        return CellTarget.class;
    }
}
