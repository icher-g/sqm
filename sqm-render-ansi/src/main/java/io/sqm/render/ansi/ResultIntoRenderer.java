package io.sqm.render.ansi;

import io.sqm.core.ResultInto;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders {@code OUTPUT / RETURNING ... INTO ...} targets.
 */
public class ResultIntoRenderer implements Renderer<ResultInto> {

    /**
     * Creates a result-into renderer.
     */
    public ResultIntoRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(ResultInto node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("INSERT / UPDATE / DELETE ... OUTPUT / RETURNING INTO", ctx.dialect().name());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ResultInto> targetType() {
        return ResultInto.class;
    }
}