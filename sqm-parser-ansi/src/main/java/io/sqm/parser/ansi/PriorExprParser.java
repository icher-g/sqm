package io.sqm.parser.ansi;

import io.sqm.core.PriorExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;

/**
 * Placeholder parser for {@code PRIOR} expressions in dialects that do not support them.
 */
public class PriorExprParser implements MatchableParser<PriorExpr> {
    /**
     * Creates an ANSI prior-expression parser.
     */
    public PriorExprParser() {
    }

    /**
     * Returns an unsupported feature parse result.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return unsupported feature result
     */
    @Override
    public ParseResult<? extends PriorExpr> parse(Cursor cur, ParseContext ctx) {
        return error("PRIOR expressions are not supported by this dialect", cur.fullPos());
    }

    /**
     * Returns this parser target type.
     *
     * @return {@link PriorExpr} class
     */
    @Override
    public Class<PriorExpr> targetType() {
        return PriorExpr.class;
    }

    /**
     * Performs a look-ahead test.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return always {@code false} for ANSI
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return false;
    }
}
