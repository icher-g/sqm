package io.sqm.parser.ansi;

import io.sqm.core.CellSelector;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for CellSelector.Condition. Oracle support follows in #506.
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
        return ParseResult.error(ctx.capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL CellSelector.Condition is not supported yet (#506)" : "MODEL CellSelector.Condition is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellSelector.Condition> targetType() {
        return CellSelector.Condition.class;
    }
}
