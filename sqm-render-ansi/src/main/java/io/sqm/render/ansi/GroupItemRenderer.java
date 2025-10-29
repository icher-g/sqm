package io.sqm.render.ansi;

import io.sqm.core.GroupItem;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class GroupItemRenderer implements Renderer<GroupItem> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(GroupItem node, RenderContext ctx, SqlWriter w) {
        if (node.isOrdinal()) {
            w.append(Integer.toString(node.ordinal()));
        } else {
            w.append(node.expr());
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<GroupItem> targetType() {
        return GroupItem.class;
    }
}
