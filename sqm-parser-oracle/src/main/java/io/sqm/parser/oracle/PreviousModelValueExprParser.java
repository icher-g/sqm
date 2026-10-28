package io.sqm.parser.oracle;

import io.sqm.core.CellRefExpr;
import io.sqm.core.PreviousModelValueExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.core.TokenType.*;

/**
 * Parses PREVIOUS(sales['Bike', 2025]); its argument must be a readable model cell.
 */
public class PreviousModelValueExprParser extends io.sqm.parser.ansi.PreviousModelValueExprParser {
    /**
     * Creates a previous-model-value parser.
     */
    public PreviousModelValueExprParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends PreviousModelValueExpr> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        cur.expect("Expected PREVIOUS", PREVIOUS);
        cur.expect("Expected ( after PREVIOUS", LPAREN);
        var cell = ModelGrammar.node(CellRefExpr.class, cur, ctx);
        cur.expect("Expected ) after PREVIOUS cell", RPAREN);
        return ParseResult.ok(PreviousModelValueExpr.of(cell));
    }
}
