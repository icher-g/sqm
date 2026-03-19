package io.sqm.render.sqlserver;

import io.sqm.core.OutputColumnExpr;
import io.sqm.core.OutputRowSource;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server {@code inserted.<column>} and {@code deleted.<column>} references.
 */
public class OutputColumnExprRenderer extends io.sqm.render.ansi.OutputColumnExprRenderer {

    /**
     * Creates a SQL Server result-column renderer.
     */
    public OutputColumnExprRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(OutputColumnExpr node, RenderContext ctx, SqlWriter w) {
        w.append(node.source() == OutputRowSource.INSERTED ? "inserted" : "deleted")
            .append(".")
            .append(renderIdentifier(node.column(), ctx.dialect().quoter()));
    }
}