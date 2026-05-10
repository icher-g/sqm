package io.sqm.render.sqlserver;

import io.sqm.core.PivotValue;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server {@code PIVOT} values.
 */
public class PivotValueRenderer extends io.sqm.render.ansi.PivotValueRenderer {
    /**
     * Creates a SQL Server pivot value renderer.
     */
    public PivotValueRenderer() {
    }

    /**
     * Renders a SQL Server pivot value.
     *
     * @param node pivot value
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    public void render(PivotValue node, RenderContext ctx, SqlWriter w) {
        if (node.alias() != null) {
            throw new UnsupportedDialectFeatureException("PIVOT value aliases", ctx.dialect().name());
        }
        super.render(node, ctx, w);
    }
}
