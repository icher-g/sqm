package io.sqm.parser.oracle;

import io.sqm.core.IterationNumberExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Parses the bare ITERATION_NUMBER atom, distinct from an ordinary column or function call.
 */
public class IterationNumberExprParser extends io.sqm.parser.ansi.IterationNumberExprParser {
    /**
     * Creates an iteration-number parser.
     */
    public IterationNumberExprParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends IterationNumberExpr> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        cur.expect("Expected ITERATION_NUMBER", TokenType.ITERATION_NUMBER);
        return ParseResult.ok(IterationNumberExpr.of());
    }
}
