package io.sqm.render.ansi;

import io.sqm.core.StarResultItem;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders star select items.
 */
public class StarResultItemRenderer implements Renderer<StarResultItem> {
    /**
     * Creates a star-select-item renderer.
     */
    public StarResultItemRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(StarResultItem node, RenderContext ctx, SqlWriter w) {
        w.append("*");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<StarResultItem> targetType() {
        return StarResultItem.class;
    }
}
