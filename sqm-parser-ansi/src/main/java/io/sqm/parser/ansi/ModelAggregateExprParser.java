package io.sqm.parser.ansi;

import io.sqm.core.ModelAggregateExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit ANSI unsupported boundary for ModelAggregateExpr; supporting dialects override it.
 */
public class ModelAggregateExprParser implements Parser<ModelAggregateExpr> {
    /**
     * Creates the ModelAggregateExpr parser.
     */
    public ModelAggregateExprParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends ModelAggregateExpr> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL ModelAggregateExpr is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<ModelAggregateExpr> targetType() {
        return ModelAggregateExpr.class;
    }
}
