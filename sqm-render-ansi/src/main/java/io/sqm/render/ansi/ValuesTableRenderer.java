package io.sqm.render.ansi;

import io.sqm.core.ValuesTable;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class ValuesTableRenderer implements Renderer<ValuesTable> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(ValuesTable node, RenderContext ctx, SqlWriter w) {
        // (VALUES (1, 'A'), (2, 'B')) AS v(id, name)
        w.append("(").append("VALUES").space().append(node.values()).append(")");

        renderAliased(node, ctx, w);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ValuesTable> targetType() {
        return ValuesTable.class;
    }
}
