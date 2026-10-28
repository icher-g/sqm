package io.sqm.parser.ansi;

import io.sqm.core.PresenceValueExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit ANSI unsupported boundary for PresenceValueExpr; supporting dialects override it.
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
        return ParseResult.error("MODEL PresenceValueExpr is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PresenceValueExpr> targetType() {
        return PresenceValueExpr.class;
    }
}
