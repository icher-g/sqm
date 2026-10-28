package io.sqm.parser.ansi;

import io.sqm.core.CurrentDimensionExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit ANSI unsupported boundary for CurrentDimensionExpr; supporting dialects override it.
 */
public class CurrentDimensionExprParser implements Parser<CurrentDimensionExpr> {
    /**
     * Creates the CurrentDimensionExpr parser.
     */
    public CurrentDimensionExprParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CurrentDimensionExpr> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL CurrentDimensionExpr is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CurrentDimensionExpr> targetType() {
        return CurrentDimensionExpr.class;
    }
}
