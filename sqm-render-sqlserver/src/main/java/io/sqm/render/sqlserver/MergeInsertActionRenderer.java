package io.sqm.render.sqlserver;

import io.sqm.core.MergeInsertAction;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server MERGE insert actions.
 */
public class MergeInsertActionRenderer extends io.sqm.render.ansi.MergeInsertActionRenderer {

    /**
     * Creates a SQL Server merge-insert-action renderer.
     */
    public MergeInsertActionRenderer() {
    }

    @Override
    public void render(MergeInsertAction node, RenderContext ctx, SqlWriter w) {
        w.append("INSERT");
        if (!node.columns().isEmpty()) {
            w.space().append("(");
            for (int i = 0; i < node.columns().size(); i++) {
                if (i > 0) {
                    w.append(", ");
                }
                w.append(renderIdentifier(node.columns().get(i), ctx.dialect().quoter()));
            }
            w.append(")");
        }
        w.space().append("VALUES").space().append("(");
        for (int i = 0; i < node.values().items().size(); i++) {
            if (i > 0) {
                w.append(", ");
            }
            w.append(node.values().items().get(i));
        }
        w.append(")");
    }
}
