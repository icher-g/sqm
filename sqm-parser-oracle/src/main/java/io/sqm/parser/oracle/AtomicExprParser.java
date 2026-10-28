package io.sqm.parser.oracle;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.MatchResult;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

/**
 * Recognizes special MODEL atoms only inside a MODEL rules scope.
 */
public class AtomicExprParser extends io.sqm.parser.AtomicExprParser {
    /**
     * Creates an Oracle atomic-expression parser.
     */
    public AtomicExprParser() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MatchResult<? extends Expression> parseDialectExpression(Cursor cur, ParseContext ctx) {
        Class<? extends Expression> type = switch (cur.peek().type()) {
            case CV -> CurrentDimensionExpr.class;
            case ITERATION_NUMBER -> IterationNumberExpr.class;
            case PREVIOUS -> PreviousModelValueExpr.class;
            case PRESENTV, PRESENTNNV -> PresenceValueExpr.class;
            default -> null;
        };
        if (type != null && !ModelGrammar.inRules(ctx)) {
            return MatchResult.matched(ParseResult.error("MODEL " + cur.peek().lexeme() + " is only valid in MODEL rules", cur.fullPos()));
        }
        return type == null ? MatchResult.notMatched() : MatchResult.matched(ctx.parse(type, cur));
    }
}
