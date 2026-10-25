package io.sqm.parser.ansi;

import io.sqm.core.ModelRules;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for ModelRules.
 */
public class ModelRulesParser implements Parser<ModelRules> {
    /**
     * Creates the ModelRules parser.
     */
    public ModelRulesParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends ModelRules> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL ModelRules is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ModelRules> targetType() {
        return ModelRules.class;
    }
}
