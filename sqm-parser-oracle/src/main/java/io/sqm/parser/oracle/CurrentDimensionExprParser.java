package io.sqm.parser.oracle;

import io.sqm.core.CurrentDimensionExpr;
import io.sqm.core.Identifier;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.core.TokenType.*;

/**
 * Parses CV() or CV(year_key), preserving an explicitly quoted dimension identifier.
 */
public class CurrentDimensionExprParser extends io.sqm.parser.ansi.CurrentDimensionExprParser {
    /**
     * Creates a current-dimension parser.
     */
    public CurrentDimensionExprParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CurrentDimensionExpr> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        cur.expect("Expected CV", CV);
        cur.expect("Expected ( after CV", LPAREN);
        Identifier dimension = cur.match(RPAREN) ? null : toIdentifier(cur.expect("Expected dimension in CV", IDENT));
        cur.expect("Expected ) after CV dimension", RPAREN);
        return ParseResult.ok(CurrentDimensionExpr.of(dimension));
    }
}
