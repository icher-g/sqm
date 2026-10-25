package io.sqm.parser.oracle;

import io.sqm.core.CellAddress;
import io.sqm.core.CellFor;
import io.sqm.core.CellSelector;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Routes a writable coordinate such as 2026 to its dedicated selector parser.
 */
public class CellAddressParser extends io.sqm.parser.ansi.CellAddressParser {
    /**
     * Creates an address parser.
     */
    public CellAddressParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellAddress> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        return cur.match(TokenType.FOR) ? ctx.parse(CellFor.class, cur) : ctx.parse(CellSelector.class, cur);
    }
}
