package io.sqm.render.ansi;

import io.sqm.core.ResultClause;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders {@code RESULT} clauses.
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
        throw new UnsupportedDialectFeatureException("INSERT / UPDATE / DELETE ... OUTPUT / RETURNING", ctx.dialect().name());
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