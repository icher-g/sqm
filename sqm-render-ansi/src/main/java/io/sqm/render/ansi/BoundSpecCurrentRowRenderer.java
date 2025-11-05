package io.sqm.render.ansi;

import io.sqm.core.BoundSpec;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class BoundSpecCurrentRowRenderer implements Renderer<BoundSpec.CurrentRow> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(BoundSpec.CurrentRow node, RenderContext ctx, SqlWriter w) {
        w.append("CURRENT ROW");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<BoundSpec.CurrentRow> targetType() {
        return BoundSpec.CurrentRow.class;
    }
}
