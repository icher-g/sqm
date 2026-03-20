package io.sqm.render.ansi;

import io.sqm.core.MergeDoNothingAction;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Baseline ANSI renderer for {@link MergeDoNothingAction}.
 */
public class MergeDoNothingActionRenderer implements Renderer<MergeDoNothingAction> {

    /**
     * Creates a merge-do-nothing-action renderer.
     */
    public MergeDoNothingActionRenderer() {
    }

    @Override
    public void render(MergeDoNothingAction node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MERGE DO NOTHING action", ctx.dialect().name());
    }

    @Override
    public Class<MergeDoNothingAction> targetType() {
        return MergeDoNothingAction.class;
    }
}
