package io.sqm.parser.ansi;

import io.sqm.core.ReferenceModel;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for ReferenceModel. Oracle support follows in #506.
 */
public class ReferenceModelParser implements Parser<ReferenceModel> {
    /**
     * Creates the ReferenceModel parser.
     */
    public ReferenceModelParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends ReferenceModel> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error(ctx.capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL ReferenceModel is not supported yet (#506)" : "MODEL ReferenceModel is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ReferenceModel> targetType() {
        return ReferenceModel.class;
    }
}
