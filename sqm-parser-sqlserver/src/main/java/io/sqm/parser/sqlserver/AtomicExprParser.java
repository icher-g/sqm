package io.sqm.parser.sqlserver;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.core.OutputColumnExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.MatchResult;
import io.sqm.parser.spi.ParseContext;

/**
 * SQL Server atomic-expression parser.
 *
 * <p>This variant preserves the shared atomic-expression flow while resolving
 * SQL Server {@code inserted.<column>} and {@code deleted.<column>} pseudo-row
 * references before ordinary {@link ColumnExpr} parsing.</p>
 */
public class AtomicExprParser extends io.sqm.parser.AtomicExprParser {

    /**
     * Creates a SQL Server atomic-expression parser.
     */
    public AtomicExprParser() {
    }

    /**
     * Attempts to parse SQL Server result pseudo-columns before falling back to
     * regular column references.
     *
     * @param cur the current token cursor
     * @param ctx the parsing context
     * @return a match result for the column-like expression phase
     */
    @Override
    protected MatchResult<? extends Expression> parseColumnLikeExpression(Cursor cur, ParseContext ctx) {
        var matched = ctx.parseIfMatch(OutputColumnExpr.class, cur);
        if (matched.match()) {
            return matched;
        }
        return ctx.parseIfMatch(ColumnExpr.class, cur);
    }
}
