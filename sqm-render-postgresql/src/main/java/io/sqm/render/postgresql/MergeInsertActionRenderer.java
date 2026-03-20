package io.sqm.render.postgresql;

import io.sqm.core.MergeInsertAction;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders PostgreSQL MERGE insert actions.
 */
public class MergeInsertActionRenderer extends io.sqm.render.ansi.MergeInsertActionRenderer {

    /**
     * Creates a PostgreSQL merge-insert-action renderer.
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
