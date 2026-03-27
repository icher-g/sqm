package io.sqm.render.sqlserver;

import io.sqm.core.CrossJoin;
import io.sqm.core.Lateral;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server cross joins, including {@code CROSS APPLY}.
 */
public final class CrossJoinRenderer extends io.sqm.render.ansi.CrossJoinRenderer {
    /**
     * Creates a SQL Server cross-join renderer.
     */
    public CrossJoinRenderer() {
    }

    @Override
    public void render(CrossJoin node, RenderContext ctx, SqlWriter w) {
        if (node.right() instanceof Lateral lateral) {
            if (!ctx.dialect().capabilities().supports(SqlFeature.LATERAL)) {
                throw new UnsupportedDialectFeatureException("APPLY", ctx.dialect().name());
            }
            w.append("CROSS APPLY").space().append(lateral.inner());
            return;
        }
        super.render(node, ctx, w);
    }
}
