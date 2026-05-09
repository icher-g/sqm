package io.sqm.parser.oracle;

import io.sqm.core.Expression;
import io.sqm.core.PriorExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Oracle parser for hierarchical-query {@code PRIOR} expressions.
 */
public class PriorExprParser extends io.sqm.parser.ansi.PriorExprParser {
    /**
     * Creates an Oracle prior-expression parser.
     */
    public PriorExprParser() {
    }

    /**
     * Checks whether the cursor starts with {@code PRIOR}.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return {@code true} when {@code PRIOR} is next
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.PRIOR);
    }

    /**
     * Parses a {@code PRIOR} expression.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return parsed prior expression
     */
    @Override
    public ParseResult<? extends PriorExpr> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected PRIOR", TokenType.PRIOR);
        var expr = ctx.parse(Expression.class, cur);
        if (expr.isError()) {
            return error(expr);
        }
        return ok(PriorExpr.of(expr.value()));
    }
}
