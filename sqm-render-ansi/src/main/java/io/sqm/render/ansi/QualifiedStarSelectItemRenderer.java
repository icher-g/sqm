package io.sqm.render.ansi;

import io.sqm.core.QualifiedStarSelectItem;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class QualifiedStarSelectItemRenderer implements Renderer<QualifiedStarSelectItem> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(QualifiedStarSelectItem node, RenderContext ctx, SqlWriter w) {
        w.append(node.qualifier()).append(".*");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<QualifiedStarSelectItem> targetType() {
        return QualifiedStarSelectItem.class;
    }
}
