package io.sqm.parser.ansi;

import io.sqm.core.PreviousModelValueExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit ANSI unsupported boundary for PreviousModelValueExpr; supporting dialects override it.
 */
public class PreviousModelValueExprParser implements Parser<PreviousModelValueExpr> {
    /**
     * Creates the PreviousModelValueExpr parser.
     */
    public PreviousModelValueExprParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends PreviousModelValueExpr> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL PreviousModelValueExpr is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PreviousModelValueExpr> targetType() {
        return PreviousModelValueExpr.class;
    }
}
