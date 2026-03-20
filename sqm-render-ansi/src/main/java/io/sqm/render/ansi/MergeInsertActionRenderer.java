package io.sqm.render.ansi;

import io.sqm.core.MergeInsertAction;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Baseline ANSI renderer for {@link MergeInsertAction}.
 */
public class MergeInsertActionRenderer implements Renderer<MergeInsertAction> {

    /**
     * Creates a merge-insert-action renderer.
     */
    public MergeInsertActionRenderer() {
    }

    @Override
    public void render(MergeInsertAction node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MERGE INSERT action", ctx.dialect().name());
    }

    @Override
    public Class<MergeInsertAction> targetType() {
        return MergeInsertAction.class;
    }
}
