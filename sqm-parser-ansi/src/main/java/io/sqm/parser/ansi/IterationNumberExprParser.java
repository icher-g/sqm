package io.sqm.parser.ansi;

import io.sqm.core.IterationNumberExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit ANSI unsupported boundary for IterationNumberExpr; supporting dialects override it.
 */
public class IterationNumberExprParser implements Parser<IterationNumberExpr> {
    /**
     * Creates the IterationNumberExpr parser.
     */
    public IterationNumberExprParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends IterationNumberExpr> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL IterationNumberExpr is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<IterationNumberExpr> targetType() {
        return IterationNumberExpr.class;
    }
}
