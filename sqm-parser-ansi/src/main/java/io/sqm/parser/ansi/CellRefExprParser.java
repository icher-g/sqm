package io.sqm.parser.ansi;

import io.sqm.core.CellRefExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for CellRefExpr.
 */
public class CellRefExprParser implements Parser<CellRefExpr> {
    /**
     * Creates the CellRefExpr parser.
     */
    public CellRefExprParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellRefExpr> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL CellRefExpr is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellRefExpr> targetType() {
        return CellRefExpr.class;
    }
}
