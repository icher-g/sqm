package io.sqm.render.ansi;

import io.sqm.core.MergeStatement;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.spi.Renderer;

/**
 * Baseline ANSI renderer for {@code MERGE} statements.
 */
public class MergeStatementRenderer implements Renderer<MergeStatement> {

    /**
     * Creates a merge-statement renderer.
     */
    public MergeStatementRenderer() {
    }

    @Override
    public void render(MergeStatement node, RenderContext ctx, SqlWriter w) {
        throw new UnsupportedDialectFeatureException("MERGE", ctx.dialect().name());
    }

    @Override
    public Class<MergeStatement> targetType() {
        return MergeStatement.class;
    }
}
