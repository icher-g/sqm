package io.sqm.render.postgresql;

import io.sqm.core.GroupItem;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders {@code CUBE (...)} items.
 */
public class CubeRenderer implements Renderer<GroupItem.Cube> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(GroupItem.Cube node, RenderContext ctx, SqlWriter w) {
        w.append("CUBE");
        GroupingRenderSupport.renderGroupingContainer(node.items(), w, true);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends GroupItem.Cube> targetType() {
        return GroupItem.Cube.class;
    }
}
