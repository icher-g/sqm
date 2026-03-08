package io.sqm.render.ansi;

import io.sqm.core.DeleteStatement;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders baseline ANSI {@code DELETE} statements.
 */
public class DeleteStatementRenderer implements Renderer<DeleteStatement> {

    /**
     * Creates a delete-statement renderer.
     */
    public DeleteStatementRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(DeleteStatement node, RenderContext ctx, SqlWriter w) {
        w.append("DELETE FROM").space().append(node.table());
        if (node.where() != null) {
            w.space().append("WHERE").space().append(node.where());
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<DeleteStatement> targetType() {
        return DeleteStatement.class;
    }
}
