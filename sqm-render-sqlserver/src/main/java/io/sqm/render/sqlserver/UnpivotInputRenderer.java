package io.sqm.render.sqlserver;

import io.sqm.core.UnpivotInput;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server unpivot input column names.
 */
public class UnpivotInputRenderer extends io.sqm.render.ansi.UnpivotInputRenderer {
    /**
     * Creates a SQL Server unpivot input renderer.
     */
    public UnpivotInputRenderer() {
    }

    /**
     * Renders the node into a SQL writer.
     *
     * @param node unpivot input
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    public void render(UnpivotInput node, RenderContext ctx, SqlWriter w) {
        var quoter = ctx.dialect().quoter();
        if (node.sourceColumns().size() == 1) {
            w.append(renderIdentifier(node.sourceColumns().getFirst(), quoter));
        }
        else {
            w.append("(").comma(node.sourceColumns(), quoter).append(")");
        }
    }
}
