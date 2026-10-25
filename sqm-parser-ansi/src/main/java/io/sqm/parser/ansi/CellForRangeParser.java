package io.sqm.parser.ansi;

import io.sqm.core.CellFor;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for CellFor.Range. Oracle support follows in #506.
 */
public class CellForRangeParser implements Parser<CellFor.Range> {
    /**
     * Creates the CellFor.Range parser.
     */
    public CellForRangeParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellFor.Range> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error(ctx.capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL CellFor.Range is not supported yet (#506)" : "MODEL CellFor.Range is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellFor.Range> targetType() {
        return CellFor.Range.class;
    }
}
