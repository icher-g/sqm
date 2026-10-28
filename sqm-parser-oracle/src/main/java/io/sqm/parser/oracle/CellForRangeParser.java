package io.sqm.parser.oracle;

import io.sqm.core.CellFor;
import io.sqm.core.Expression;
import io.sqm.core.RangeDirection;
import io.sqm.core.oracle.internal.ModelSyntaxChecker;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.List;

import static io.sqm.parser.core.TokenType.*;

/**
 * Parses FOR year_key FROM 2025 TO 2027 INCREMENT 1 and FOR product LIKE 'P%' FROM 1 TO 3 INCREMENT 1.
 */
public class CellForRangeParser extends io.sqm.parser.ansi.CellForRangeParser {
    /**
     * Creates a range-generator parser.
     */
    public CellForRangeParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends CellFor.Range> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        cur.expect("Expected FOR", FOR);
        var dimension = toIdentifier(cur.expect("Expected FOR dimension", IDENT));
        ModelGrammar.dimensions(List.of(dimension), cur, ctx);
        var pattern = cur.consumeIf(LIKE) ? ModelGrammar.node(Expression.class, cur, ctx) : null;
        cur.expect("Expected FROM in FOR range", FROM);
        var from = ModelGrammar.node(Expression.class, cur, ctx);
        cur.expect("Expected TO in FOR range", TO);
        var to = ModelGrammar.node(Expression.class, cur, ctx);
        var direction = cur.expect("Expected INCREMENT or DECREMENT", INCREMENT, DECREMENT).type() == INCREMENT
            ? RangeDirection.INCREMENT
            : RangeDirection.DECREMENT;
        var step = ModelGrammar.node(Expression.class, cur, ctx);
        var result = CellFor.Range.of(dimension, pattern, from, to, direction, step);
        ModelSyntaxChecker.range(result);
        return ParseResult.ok(result);
    }
}
