package io.sqm.render.ansi;

import io.sqm.core.OutputItem;
import io.sqm.core.QueryExpr;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders one projected item inside an {@code OUTPUT} clause.
 */
public class OutputItemRenderer implements Renderer<OutputItem> {

    /**
     * Creates an output-item renderer.
     */
    public OutputItemRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(OutputItem node, RenderContext ctx, SqlWriter w) {
        w.append(node.expression(), node.expression() instanceof QueryExpr, true);
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
    public Class<OutputItem> targetType() {
        return OutputItem.class;
    }
}