package io.sqm.render.ansi;

import io.sqm.core.OutputClause;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders SQL Server-style {@code OUTPUT} clauses.
 */
public class OutputClauseRenderer implements Renderer<OutputClause> {

    /**
     * Creates an output-clause renderer.
     */
    public OutputClauseRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(OutputClause node, RenderContext ctx, SqlWriter w) {
        w.append("OUTPUT").space();
        w.comma(node.items());
        if (node.into() != null) {
            w.space().append(node.into());
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OutputClause> targetType() {
        return OutputClause.class;
    }
}