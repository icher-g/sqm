package io.sqm.parser.ansi;

import io.sqm.core.PreviousModelValueExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for PreviousModelValueExpr. Oracle support follows in #506.
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
        return ParseResult.error(ctx.capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL PreviousModelValueExpr is not supported yet (#506)" : "MODEL PreviousModelValueExpr is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PreviousModelValueExpr> targetType() {
        return PreviousModelValueExpr.class;
    }
}
