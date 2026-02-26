package io.sqm.render.ansi;

import io.sqm.core.Lateral;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Renders LATERAL table references.
 */
public class LateralRenderer implements Renderer<Lateral> {
    /**
     * Creates a lateral-table renderer.
     */
    public LateralRenderer() {
    }

    /**
     * Renders the node into an {@link SqlWriter}.
     *
     * @param node a node to render.
     * @param ctx  a render context.
     * @param w    a writer.
     */
    @Override
    public void render(Lateral node, RenderContext ctx, SqlWriter w) {
        if (!ctx.dialect().capabilities().supports(SqlFeature.LATERAL)) {
            throw new UnsupportedDialectFeatureException("LATERAL", ctx.dialect().name());
        }
        w.append("LATERAL").space().append(node.inner());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends Lateral> targetType() {
        return Lateral.class;
    }
}
