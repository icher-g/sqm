package io.sqm.render.sqlserver;

import io.sqm.core.ResultClause;
import io.sqm.core.RelationResultTarget;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders SQL Server-style {@code OUTPUT} clauses.
 */
public class ResultClauseRenderer implements Renderer<ResultClause> {

    /**
     * Creates a result-clause renderer.
     */
    public ResultClauseRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(ResultClause node, RenderContext ctx, SqlWriter w) {
        w.append("OUTPUT").space();
        w.comma(node.items());
        if (node.target() != null) {
            if (!(node.target() instanceof RelationResultTarget)) {
                throw new UnsupportedOperationException("SQL Server OUTPUT supports only relation targets in OUTPUT INTO");
            }
            w.space().append(node.target());
        }
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ResultClause> targetType() {
        return ResultClause.class;
    }
}
