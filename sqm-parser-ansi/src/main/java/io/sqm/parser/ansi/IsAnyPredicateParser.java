package io.sqm.parser.ansi;

import io.sqm.core.IsAnyPredicate;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Explicit unsupported boundary for IsAnyPredicate. Oracle support follows in #506.
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
        return ParseResult.error(ctx.capabilities().supports(SqlFeature.MODEL_CLAUSE) ? "MODEL IsAnyPredicate is not supported yet (#506)" : "MODEL IsAnyPredicate is not supported by this dialect", cur.fullPos());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<IsAnyPredicate> targetType() {
        return IsAnyPredicate.class;
    }
}
