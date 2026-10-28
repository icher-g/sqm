package io.sqm.parser.ansi;

import io.sqm.core.CellPresentPredicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit ANSI unsupported boundary for CellPresentPredicate; supporting dialects override it.
 */
public class CellPresentPredicateParser implements Parser<CellPresentPredicate> {
    /**
     * Creates the CellPresentPredicate parser.
     */
    public CellPresentPredicateParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellPresentPredicate> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL CellPresentPredicate is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<CellPresentPredicate> targetType() {
        return CellPresentPredicate.class;
    }
}
