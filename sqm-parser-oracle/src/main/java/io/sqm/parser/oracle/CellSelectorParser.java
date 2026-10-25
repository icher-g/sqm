package io.sqm.parser.oracle;

import io.sqm.core.CellFor;
import io.sqm.core.CellSelector;
import io.sqm.core.IsAnyPredicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Routes a positional coordinate such as 2026; symbolic selectors follow in #506.
 */
public class CellSelectorParser extends io.sqm.parser.ansi.CellSelectorParser {
    /**
     * Creates a selector parser.
     */
    public CellSelectorParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellSelector> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        if (cur.match(TokenType.ANY)) return ParseResult.error(ctx.parse(IsAnyPredicate.class, cur));
        if (cur.match(TokenType.FOR)) return ParseResult.error(ctx.parse(CellFor.class, cur));
        return ctx.parse(CellSelector.Value.class, cur);
    }
}
