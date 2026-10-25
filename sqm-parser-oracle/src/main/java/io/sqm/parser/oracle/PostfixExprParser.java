package io.sqm.parser.oracle;

import io.sqm.core.CellRefExpr;
import io.sqm.core.Expression;
import io.sqm.core.FunctionExpr;
import io.sqm.core.ModelAggregateExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchResult;
import io.sqm.parser.spi.ParseContext;

/**
 * Distinguishes MODEL cell brackets from ordinary array postfix syntax.
 */
public class PostfixExprParser extends io.sqm.parser.PostfixExprParser {
    /**
     * Creates an Oracle postfix parser.
     *
     * @param atomic parser for the initial expression
     */
    public PostfixExprParser(AtomicExprParser atomic) {
        super(atomic);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MatchResult<? extends Expression> parseDialectPostfix(Expression lhs, Cursor cur, ParseContext ctx) {
        if (!(ctx instanceof ModelParseContext local) || local.scope() == null || !cur.match(TokenType.LBRACKET)) {
            return MatchResult.notMatched();
        }
        if (lhs instanceof FunctionExpr) return MatchResult.matched(ctx.parse(ModelAggregateExpr.class, cur));
        return MatchResult.matched(ctx.parse(CellRefExpr.class, lhs, cur));
    }
}
