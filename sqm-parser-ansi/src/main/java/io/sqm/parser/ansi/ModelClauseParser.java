package io.sqm.parser.ansi;

import io.sqm.core.ModelClause;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for ModelClause.
 */
public class ModelClauseParser implements Parser<ModelClause> {
    /**
     * Creates the ModelClause parser.
     */
    public ModelClauseParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends ModelClause> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL ModelClause is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ModelClause> targetType() {
        return ModelClause.class;
    }
}
