package io.sqm.parser.oracle;

import io.sqm.core.CellFor;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.core.TokenType.*;

/**
 * Routes FOR year_key IN (...) and FOR year_key FROM ... TO ... to their dedicated parsers.
 */
public class CellForParser extends io.sqm.parser.ansi.CellForParser {
    /**
     * Creates a FOR-generator router.
     */
    public CellForParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellFor> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        // Probe the header only; the selected child owns and consumes the full fragment.
        int mark = cur.mark();
        cur.expect("Expected FOR", FOR);
        if (cur.match(LPAREN)) {
            cur.restore(mark);
            return ctx.parse(CellFor.Values.class, cur);
        }
        cur.expect("Expected dimension after FOR", IDENT);
        boolean values = cur.match(IN);
        cur.restore(mark);
        return values ? ctx.parse(CellFor.Values.class, cur) : ctx.parse(CellFor.Range.class, cur);
    }
}
