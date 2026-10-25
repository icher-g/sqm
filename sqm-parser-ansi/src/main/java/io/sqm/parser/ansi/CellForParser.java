package io.sqm.parser.ansi;

import io.sqm.core.CellFor;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for CellFor. Oracle support follows in #506.
 */
public class CellForParser implements Parser<CellFor> {
    /**
     * Creates the CellFor parser.
     */
    public CellForParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellFor> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error(ctx.capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL CellFor is not supported yet (#506)" : "MODEL CellFor is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellFor> targetType() {
        return CellFor.class;
    }
}
