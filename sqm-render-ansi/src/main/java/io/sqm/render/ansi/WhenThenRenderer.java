package io.sqm.render.ansi;

import io.sqm.core.WhenThen;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class WhenThenRenderer implements Renderer<WhenThen> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(WhenThen node, RenderContext ctx, SqlWriter w) {
        w.space().append("WHEN").space();
        w.append(node.when());
        w.space().append("THEN").space();
        w.append(node.then());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<WhenThen> targetType() {
        return WhenThen.class;
    }
}
