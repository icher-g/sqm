package io.sqm.render.ansi;

import io.sqm.core.ExprResultItem;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders one projected item inside an {@code OUTPUT / RETURN} clause.
 */
public class ExprResultItemRenderer implements Renderer<ExprResultItem> {

    /**
     * Creates an result-item renderer.
     */
    public ExprResultItemRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(ExprResultItem node, RenderContext ctx, SqlWriter w) {
        w.append(node.expr());
        if (node.alias() != null) {
            w.space().append("AS").space().append(renderIdentifier(node.alias(), ctx.dialect().quoter()));
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ExprResultItem> targetType() {
        return ExprResultItem.class;
    }
}