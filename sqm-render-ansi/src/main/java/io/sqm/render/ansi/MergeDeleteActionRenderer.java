package io.sqm.render.ansi;

import io.sqm.core.MergeDeleteAction;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Baseline ANSI renderer for {@link MergeDeleteAction}.
 */
public class MergeDeleteActionRenderer implements Renderer<MergeDeleteAction> {

    /**
     * Creates a merge-delete-action renderer.
     */
    public MergeDeleteActionRenderer() {
    }

    @Override
    public void render(MergeDeleteAction node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MERGE DELETE action", ctx.dialect().name());
    }

    @Override
    public Class<MergeDeleteAction> targetType() {
        return MergeDeleteAction.class;
    }
}
