package io.sqm.render.ansi;

import io.sqm.core.OutputInto;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders SQL Server-style {@code OUTPUT ... INTO ...} targets.
 */
public class OutputIntoRenderer implements Renderer<OutputInto> {

    /**
     * Creates an output-into renderer.
     */
    public OutputIntoRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(OutputInto node, RenderContext ctx, SqlWriter w) {
        w.append("INTO").space().append(node.target());
        if (!node.columns().isEmpty()) {
            w.space().append("(");
            w.comma(node.columns(), ctx.dialect().quoter());
            w.append(")");
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OutputInto> targetType() {
        return OutputInto.class;
    }
}