package io.sqm.render.ansi;

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
        throw new UnsupportedOperationException(
            "LATERAL is not supported by ANSI SQL renderer"
        );
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
