package io.sqm.parser.oracle;

import io.sqm.core.Expression;
import io.sqm.core.IterationSpec;
import io.sqm.core.Predicate;
import io.sqm.core.oracle.internal.ModelSyntaxChecker;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.core.TokenType.*;

/**
 * Parses {@code ITERATE (100)} and {@code ITERATE (100) UNTIL (ABS(PREVIOUS(sales[1]) - sales[1]) < 1)}.
 */
public class IterationSpecParser extends io.sqm.parser.ansi.IterationSpecParser {
    /**
     * Creates an iteration-specification parser.
     */
    public IterationSpecParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParseResult<? extends IterationSpec> parse(Cursor cur, ParseContext ctx) {
        ModelGrammar.require(cur, ctx);
        cur.expect("Expected ITERATE", ITERATE);
        cur.expect("Expected ( after ITERATE", LPAREN);
        var limit = ModelGrammar.node(Expression.class, cur, ctx);
        cur.expect("Expected ) after iteration limit", RPAREN);
        Predicate until = null;
        if (cur.consumeIf(UNTIL)) {
            cur.expect("Expected ( after UNTIL", LPAREN);
            until = ModelGrammar.node(Predicate.class, cur, ctx);
            cur.expect("Expected ) after UNTIL condition", RPAREN);
        }
        var iteration = IterationSpec.of(limit, until);
        ModelSyntaxChecker.iteration(iteration);
        return ParseResult.ok(iteration);
    }
}
