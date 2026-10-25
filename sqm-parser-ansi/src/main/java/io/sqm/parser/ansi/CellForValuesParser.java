package io.sqm.parser.ansi;

import io.sqm.core.CellFor;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for CellFor.Values. Oracle support follows in #506.
 */
public class CellForValuesParser implements Parser<CellFor.Values> {
    /**
     * Creates the CellFor.Values parser.
     */
    public CellForValuesParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellFor.Values> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error(ctx.capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL CellFor.Values is not supported yet (#506)" : "MODEL CellFor.Values is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellFor.Values> targetType() {
        return CellFor.Values.class;
    }
}
