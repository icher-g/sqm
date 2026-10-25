package io.sqm.parser.oracle;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.MatchResult;
import io.sqm.parser.spi.ParseContext;

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
        if (!(ctx instanceof ModelParseContext local) || local.scope() == null) return MatchResult.notMatched();
        Class<? extends Expression> type = switch (cur.peek().type()) {
            case CV -> CurrentDimensionExpr.class;
            case ITERATION_NUMBER -> IterationNumberExpr.class;
            case PREVIOUS -> PreviousModelValueExpr.class;
            case PRESENTV, PRESENTNNV -> PresenceValueExpr.class;
            case ANY -> IsAnyPredicate.class;
            default -> null;
        };
        return type == null ? MatchResult.notMatched() : MatchResult.matched(ctx.parse(type, cur));
    }
}
