package io.sqm.parser.ansi;

import io.sqm.core.MainModel;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for MainModel.
 */
public class MainModelParser implements Parser<MainModel> {
    /**
     * Creates the MainModel parser.
     */
    public MainModelParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends MainModel> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL MainModel is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<MainModel> targetType() {
        return MainModel.class;
    }
}
