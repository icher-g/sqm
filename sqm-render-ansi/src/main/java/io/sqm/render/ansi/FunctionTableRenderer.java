package io.sqm.render.ansi;

import io.sqm.core.FunctionTable;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

public class FunctionTableRenderer implements Renderer<FunctionTable> {
    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(FunctionTable node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedOperationException(
            "Function call in FROM statement is not supported by ANSI SQL renderer"
        );
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends FunctionTable> targetType() {
        return FunctionTable.class;
    }
}
