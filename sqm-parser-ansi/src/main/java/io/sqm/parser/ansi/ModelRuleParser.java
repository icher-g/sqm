package io.sqm.parser.ansi;

import io.sqm.core.ModelRule;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for ModelRule.
 */
public class ModelRuleParser implements Parser<ModelRule> {
    /**
     * Creates the ModelRule parser.
     */
    public ModelRuleParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends ModelRule> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL ModelRule is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ModelRule> targetType() {
        return ModelRule.class;
    }
}
