package io.sqm.render.ansi;

import io.sqm.core.StarSelectItem;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders star select items.
 */
public class StarSelectItemRenderer implements Renderer<StarSelectItem> {
    /**
     * Creates a star-select-item renderer.
     */
    public StarSelectItemRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(StarSelectItem node, RenderContext ctx, SqlWriter w) {
        w.append("*");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<StarSelectItem> targetType() {
        return StarSelectItem.class;
    }
}
