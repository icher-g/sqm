package io.sqm.render.ansi;

import io.sqm.core.QualifiedStarResultItem;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders qualified star select items.
 */
public class QualifiedStarResultItemRenderer implements Renderer<QualifiedStarResultItem> {
    /**
     * Creates a qualified-star select-item renderer.
     */
    public QualifiedStarResultItemRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(QualifiedStarResultItem node, RenderContext ctx, SqlWriter w) {
        w.append(renderIdentifier(node.qualifier(), ctx.dialect().quoter())).append(".*");
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<QualifiedStarResultItem> targetType() {
        return QualifiedStarResultItem.class;
    }
}
