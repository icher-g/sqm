package io.sqm.parser.ansi;

import io.sqm.core.IsAnyPredicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit ANSI unsupported boundary for IsAnyPredicate; supporting dialects override it.
 */
public class IsAnyPredicateParser implements Parser<IsAnyPredicate> {
    /**
     * Creates the IsAnyPredicate parser.
     */
    public IsAnyPredicateParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends IsAnyPredicate> parse(Cursor cur, ParseContext ctx) {
        return ParseResult.error("MODEL IsAnyPredicate is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<IsAnyPredicate> targetType() {
        return IsAnyPredicate.class;
    }
}
