package io.sqm.parser.ansi;

import io.sqm.core.PresenceValueExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for PresenceValueExpr. Oracle support follows in #506.
 */
public class PresenceValueExprParser implements Parser<PresenceValueExpr> {
    /**
     * Creates the PresenceValueExpr parser.
     */
    public PresenceValueExprParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends PresenceValueExpr> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error(ctx.capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL PresenceValueExpr is not supported yet (#506)" : "MODEL PresenceValueExpr is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PresenceValueExpr> targetType() {
        return PresenceValueExpr.class;
    }
}
