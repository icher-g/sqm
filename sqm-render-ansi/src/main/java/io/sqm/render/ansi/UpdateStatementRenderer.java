package io.sqm.render.ansi;

import io.sqm.core.UpdateStatement;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders baseline ANSI {@code UPDATE} statements.
 */
public class UpdateStatementRenderer implements Renderer<UpdateStatement> {

    /**
     * Creates an update-statement renderer.
     */
    public UpdateStatementRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(UpdateStatement node, RenderContext ctx, SqlWriter w) {
        w.append("UPDATE").space().append(node.table());
        w.space().append("SET").space();
        w.comma(node.assignments());

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
    public Class<UpdateStatement> targetType() {
        return UpdateStatement.class;
    }
}
