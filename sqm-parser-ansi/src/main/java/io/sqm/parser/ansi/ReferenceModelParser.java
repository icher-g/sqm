package io.sqm.parser.ansi;

import io.sqm.core.ReferenceModel;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit ANSI unsupported boundary for ReferenceModel; supporting dialects override it.
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
        return ParseResult.error("MODEL ReferenceModel is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ReferenceModel> targetType() {
        return ReferenceModel.class;
    }
}
