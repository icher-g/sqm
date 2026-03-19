package io.sqm.render.sqlserver;

import io.sqm.core.FunctionExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * SQL Server-specific function renderer.
 *
 * <p>This renderer keeps the shared ANSI rendering behavior and only customizes
 * the datepart argument for {@code DATEADD(...)} and {@code DATEDIFF(...)} so a
 * string literal datepart is emitted without string quoting.</p>
 */
public class FunctionExprRenderer extends io.sqm.render.ansi.FunctionExprRenderer {
    /**
     * Creates a SQL Server function renderer.
     */
    public FunctionExprRenderer() {
    }

    /**
     * Renders one SQL Server function argument, emitting the first datepart
     * argument for {@code DATEADD(...)} and {@code DATEDIFF(...)} without quotes.
     *
     * @param node function expression to render.
     * @param arg current argument.
     * @param index zero-based argument index.
     * @param ctx render context.
     * @param w SQL writer.
     */
    @Override
    protected void renderArgument(
        FunctionExpr node,
        FunctionExpr.Arg arg,
        int index,
        RenderContext ctx,
        SqlWriter w
    ) {
        var datePart = datePartLiteral(node, arg, index);
        if (datePart != null) {
            w.append(datePart);
            return;
        }
        super.renderArgument(node, arg, index, ctx, w);
    }

    private static boolean usesSqlServerDatePartRendering(FunctionExpr node) {
        var functionName = node.name().parts().getLast().value().toLowerCase(java.util.Locale.ROOT);
        return "dateadd".equals(functionName) || "datediff".equals(functionName);
    }

    private static String datePartLiteral(FunctionExpr node, FunctionExpr.Arg arg, int index) {
        if (index != 0 || !usesSqlServerDatePartRendering(node)) {
            return null;
        }
        return arg.<String>matchArg()
            .exprArg(exprArg -> exprArg.expr().<String>matchExpression()
                .literal(literal -> literal.value() instanceof String value ? value : null)
                .otherwise(ignored -> null))
            .otherwise(ignored -> null);
    }
}
