package io.sqm.render.ansi;

import io.sqm.core.MergeClause;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Baseline ANSI renderer for {@link MergeClause}.
 */
public class MergeClauseRenderer implements Renderer<MergeClause> {

    /**
     * Creates a merge-clause renderer.
     */
    public MergeClauseRenderer() {
    }

    @Override
    public void render(MergeClause node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MERGE clause", ctx.dialect().name());
    }

    @Override
    public Class<MergeClause> targetType() {
        return MergeClause.class;
    }
}
