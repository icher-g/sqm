package io.sqm.render.sqlserver;

import io.sqm.core.PivotMeasure;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.render.SqlWriter;
import io.sqm.render.spi.RenderContext;

/**
 * Renders SQL Server {@code PIVOT} measures.
 */
public class PivotMeasureRenderer extends io.sqm.render.ansi.PivotMeasureRenderer {
    /**
     * Creates a SQL Server pivot measure renderer.
     */
    public PivotMeasureRenderer() {
    }

    /**
     * Renders a SQL Server pivot measure.
     *
     * @param node pivot measure
     * @param ctx render context
     * @param w SQL writer
     */
    @Override
    public void render(PivotMeasure node, RenderContext ctx, SqlWriter w) {
        if (node.alias() != null) {
            throw new UnsupportedDialectFeatureException("PIVOT measure aliases", ctx.dialect().name());
        }
        super.render(node, ctx, w);
    }
}
