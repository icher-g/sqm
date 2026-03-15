package io.sqm.parser.sqlserver;

import io.sqm.core.FunctionExpr;
import io.sqm.core.LiteralExpr;
import io.sqm.core.QualifiedName;
import io.sqm.parser.ansi.FunctionExprParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.ok;

/**
 * SQL Server-specific function parser.
 *
 * <p>This parser keeps the shared ANSI function grammar and only customizes
 * datepart handling for {@code DATEADD(...)} and {@code DATEDIFF(...)} so the
 * first argument is modeled as a string literal instead of a column reference.</p>
 */
public class SqlServerFunctionExprParser extends FunctionExprParser {
    private static final String DATEADD = "dateadd";
    private static final String DATEDIFF = "datediff";

    /**
     * Creates a SQL Server function parser.
     */
    public SqlServerFunctionExprParser() {
    }

    /**
     * Parses one SQL Server function argument, customizing datepart handling
     * for {@code DATEADD(...)} and {@code DATEDIFF(...)}.
     *
     * @param index zero-based argument index.
     * @param cur cursor positioned at the argument.
     * @param ctx parser context.
     * @param functionName parsed qualified function name.
     * @return parsed function argument.
     */
    @Override
    protected ParseResult<? extends FunctionExpr.Arg> parseArgument(
        int index,
        Cursor cur,
        ParseContext ctx,
        QualifiedName functionName
    ) {
        if (expectsSqlServerDatePart(functionName, index) && cur.match(TokenType.IDENT)) {
            var datePart = cur.advance();
            return ok(FunctionExpr.Arg.expr(LiteralExpr.of(datePart.lexeme())));
        }
        return super.parseArgument(index, cur, ctx, functionName);
    }

    private static boolean expectsSqlServerDatePart(io.sqm.core.QualifiedName functionName, int index) {
        var normalizedFunctionName = functionName.parts().getLast().value().toLowerCase(java.util.Locale.ROOT);
        return index == 0 && (DATEADD.equals(normalizedFunctionName) || DATEDIFF.equals(normalizedFunctionName));
    }
}
