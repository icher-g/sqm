package io.sqm.render.pgsql;

import io.sqm.core.FunctionTable;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class FunctionTableRenderer implements Renderer<FunctionTable> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(FunctionTable node, RenderContext ctx, SqlWriter w) {
        w.append(node.function());
        renderAliased(node, ctx, w);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends FunctionTable> targetType() {
        return FunctionTable.class;
    }
}
