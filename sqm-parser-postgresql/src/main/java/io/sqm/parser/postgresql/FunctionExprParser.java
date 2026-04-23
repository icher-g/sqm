package io.sqm.parser.postgresql;

import io.sqm.core.FunctionExpr;
import io.sqm.core.OrderBy;
import io.sqm.core.QualifiedName;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * PostgreSQL-specific function expression parser.
 *
 * <p>This parser supports aggregate input ordering inside the function
 * argument list, for example {@code ARRAY_AGG(x ORDER BY x)}.</p>
 */
public class FunctionExprParser extends io.sqm.parser.ansi.FunctionExprParser {
    /**
     * Creates a PostgreSQL function expression parser.
     */
    public FunctionExprParser() {
    }

    /**
     * Parses optional PostgreSQL aggregate input ordering inside function
     * parentheses.
     *
     * @param cur cursor positioned after parsed arguments.
     * @param ctx parser context.
     * @param functionName parsed qualified function name.
     * @param args parsed function arguments.
     * @return parsed order-by clause or {@code null}.
     */
    @Override
    protected ParseResult<? extends OrderBy> parseOrderBy(
        Cursor cur,
        ParseContext ctx,
        QualifiedName functionName,
        List<FunctionExpr.Arg> args
    ) {
        if (!cur.match(TokenType.ORDER)) {
            return ok(null);
        }
        if (args.isEmpty()) {
            return error("Expected function argument before ORDER BY", cur.fullPos());
        }
        return ctx.parse(OrderBy.class, cur);
    }
}
