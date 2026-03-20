package io.sqm.render.ansi;

import io.sqm.core.MergeUpdateAction;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Baseline ANSI renderer for {@link MergeUpdateAction}.
 */
public class MergeUpdateActionRenderer implements Renderer<MergeUpdateAction> {

    /**
     * Creates a merge-update-action renderer.
     */
    public MergeUpdateActionRenderer() {
    }

    @Override
    public void render(MergeUpdateAction node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MERGE UPDATE action", ctx.dialect().name());
    }

    @Override
    public Class<MergeUpdateAction> targetType() {
        return MergeUpdateAction.class;
    }
}
