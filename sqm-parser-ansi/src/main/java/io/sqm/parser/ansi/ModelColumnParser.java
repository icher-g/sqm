package io.sqm.parser.ansi;

import io.sqm.core.ModelColumn;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for ModelColumn.
 */
public class ModelColumnParser implements Parser<ModelColumn> {
    /**
     * Creates the ModelColumn parser.
     */
    public ModelColumnParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends ModelColumn> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL ModelColumn is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ModelColumn> targetType() {
        return ModelColumn.class;
    }
}
