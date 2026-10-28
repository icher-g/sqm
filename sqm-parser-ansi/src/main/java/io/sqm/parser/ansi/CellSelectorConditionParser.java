package io.sqm.parser.ansi;

import io.sqm.core.CellSelector;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit ANSI unsupported boundary for CellSelector.Condition; supporting dialects override it.
 */
public class CellSelectorConditionParser implements Parser<CellSelector.Condition> {
    /**
     * Creates the CellSelector.Condition parser.
     */
    public CellSelectorConditionParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellSelector.Condition> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL CellSelector.Condition is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellSelector.Condition> targetType() {
        return CellSelector.Condition.class;
    }
}
