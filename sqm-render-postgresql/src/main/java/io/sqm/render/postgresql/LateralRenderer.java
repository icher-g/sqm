package io.sqm.render.postgresql;

import io.sqm.core.Lateral;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class LateralRenderer implements Renderer<Lateral> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(Lateral node, RenderContext ctx, SqlWriter w) {
        w.append("LATERAL").space().append(node.inner());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends Lateral> targetType() {
        return Lateral.class;
    }
}
