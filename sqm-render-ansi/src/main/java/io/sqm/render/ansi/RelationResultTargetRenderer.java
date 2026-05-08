package io.sqm.render.ansi;

import io.sqm.core.RelationResultTarget;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders {@code OUTPUT / RETURNING ... INTO ...} targets.
 */
public class RelationResultTargetRenderer implements Renderer<RelationResultTarget> {

    /**
     * Creates a result-into renderer.
     */
    public RelationResultTargetRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(RelationResultTarget node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("INSERT / UPDATE / DELETE ... OUTPUT / RETURNING INTO", ctx.dialect().name());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<RelationResultTarget> targetType() {
        return RelationResultTarget.class;
    }
}