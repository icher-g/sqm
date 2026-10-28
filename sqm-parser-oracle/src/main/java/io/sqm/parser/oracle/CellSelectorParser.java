package io.sqm.parser.oracle;

import io.sqm.core.CellSelector;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Routes positional coordinates and symbolic predicates to their registered variant parsers.
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
        if (cur.match(TokenType.FOR)) return ParseResult.error("MODEL FOR generators are only valid in writable targets", cur.fullPos());
        var mark = cur.mark();
        var condition = ctx.parse(CellSelector.Condition.class, cur);
        if (condition.ok()) return condition;
        cur.restore(mark);
        return ctx.parse(CellSelector.Value.class, cur);
    }
}
