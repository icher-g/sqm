package io.sqm.parser.oracle;

import io.sqm.core.CellRefExpr;
import io.sqm.core.Expression;
import io.sqm.core.PresenceMode;
import io.sqm.core.PresenceValueExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.core.TokenType.*;

/**
 * Parses PRESENTV(sales[2025], 100, 0) or PRESENTNNV(sales[2025], 100, 0).
 */
public class PresenceValueExprParser extends io.sqm.parser.ansi.PresenceValueExprParser {
    /**
     * Creates a conditional-presence-value parser.
     */
    public PresenceValueExprParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends PresenceValueExpr> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        var mode = cur.expect("Expected PRESENTV or PRESENTNNV", PRESENTV, PRESENTNNV).type() == PRESENTV
            ? PresenceMode.CELL
            : PresenceMode.NON_NULL_VALUE;
        cur.expect("Expected ( after presence function", LPAREN);
        var cell = ModelGrammar.node(CellRefExpr.class, cur, ctx);
        cur.expect("Expected , after presence cell", COMMA);
        var present = ModelGrammar.node(Expression.class, cur, ctx);
        cur.expect("Expected , before absent value", COMMA);
        var absent = ModelGrammar.node(Expression.class, cur, ctx);
        cur.expect("Expected ) after presence arguments", RPAREN);
        return ParseResult.ok(PresenceValueExpr.of(mode, cell, present, absent));
    }
}
